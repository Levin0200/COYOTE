package com.coyote.moyeon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 SMTP 대화(접속, 발신·수신자, 본문 전송)를 가짜 SMTP 서버로 통과시킨다.
 * Gmail 같은 실제 서버로의 발송은 계정이 필요해서 따로 직접 확인한다.
 */
@SpringBootTest(properties = {
		"app.mail.provider=smtp",
		"app.mail.from=sender@example.com",
		"spring.mail.username=sender@example.com",
		"spring.mail.password=test-password",
		// 가짜 서버는 인증과 암호화 없이 받는다.
		"spring.mail.properties.mail.smtp.auth=false",
		"spring.mail.properties.mail.smtp.starttls.enable=false",
		"spring.mail.properties.mail.smtp.starttls.required=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmtpEmailSenderIntegrationTest {

	private static final FakeSmtpServer SERVER = new FakeSmtpServer();

	@DynamicPropertySource
	static void smtpServer(DynamicPropertyRegistry registry) {
		registry.add("spring.mail.host", () -> "127.0.0.1");
		registry.add("spring.mail.port", SERVER::port);
	}

	@AfterAll
	static void stopServer() {
		SERVER.close();
	}

	@Autowired MockMvc mockMvc;
	@Autowired EmailVerificationRepository verificationRepository;
	@Autowired EmailSender emailSender;

	@Test
	void theVerificationCodeIsDeliveredOverSmtp() throws Exception {
		assertThat(emailSender).isInstanceOf(SmtpEmailSender.class);

		mockMvc.perform(post("/api/auth/email-verification").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"smtp-guest@example.com\"}")).andExpect(status().isNoContent());

		String code = verificationRepository.findByEmail("smtp-guest@example.com").orElseThrow().getCode();
		assertThat(SERVER.messages()).hasSize(1);
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
				new ByteArrayInputStream(SERVER.messages().get(0).getBytes(StandardCharsets.ISO_8859_1)));
		assertThat(((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress())
				.isEqualTo("smtp-guest@example.com");
		InternetAddress from = (InternetAddress) message.getFrom()[0];
		assertThat(from.getAddress()).isEqualTo("sender@example.com");
		// 설정 파일의 기본값이 한글 그대로 깨지지 않고 보낸 사람 이름으로 나가는지(.properties는 ISO-8859-1로 읽힌다)
		assertThat(from.getPersonal()).isEqualTo("모여ON");
		assertThat(message.getSubject()).isEqualTo("[모여ON] 이메일 인증번호");
		assertThat((String) message.getContent()).contains(code).contains("5분");
		assertThat(SERVER.envelopeRecipients()).containsExactly("smtp-guest@example.com");
	}

	/** 한 번에 한 연결만 받는 최소한의 SMTP 서버. 받은 메일 원문을 모아 둔다. */
	private static final class FakeSmtpServer implements AutoCloseable {

		private final ServerSocket socket;
		private final List<String> messages = new CopyOnWriteArrayList<>();
		private final List<String> recipients = new CopyOnWriteArrayList<>();

		FakeSmtpServer() {
			try {
				socket = new ServerSocket(0, 5, java.net.InetAddress.getLoopbackAddress());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			Thread acceptor = new Thread(this::acceptLoop, "fake-smtp");
			acceptor.setDaemon(true);
			acceptor.start();
		}

		int port() {
			return socket.getLocalPort();
		}

		List<String> messages() {
			return messages;
		}

		List<String> envelopeRecipients() {
			return recipients;
		}

		private void acceptLoop() {
			while (!socket.isClosed()) {
				try (Socket client = socket.accept()) {
					handle(client);
				} catch (IOException e) {
					return;
				}
			}
		}

		private void handle(Socket client) throws IOException {
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
			Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.ISO_8859_1);
			reply(out, "220 fake-smtp ready");
			String line;
			while ((line = in.readLine()) != null) {
				String command = line.toUpperCase();
				if (command.startsWith("EHLO") || command.startsWith("HELO")) {
					reply(out, "250-fake-smtp\r\n250 8BITMIME");
				} else if (command.startsWith("MAIL FROM")) {
					reply(out, "250 OK");
				} else if (command.startsWith("RCPT TO")) {
					recipients.add(line.substring(line.indexOf('<') + 1, line.indexOf('>')));
					reply(out, "250 OK");
				} else if (command.startsWith("DATA")) {
					reply(out, "354 End data with <CR><LF>.<CR><LF>");
					StringBuilder data = new StringBuilder();
					while ((line = in.readLine()) != null && !line.equals(".")) {
						data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
					}
					messages.add(data.toString());
					reply(out, "250 OK queued");
				} else if (command.startsWith("QUIT")) {
					reply(out, "221 bye");
					return;
				} else {
					reply(out, "250 OK");
				}
			}
		}

		private static void reply(Writer out, String text) throws IOException {
			out.write(text + "\r\n");
			out.flush();
		}

		@Override
		public void close() {
			try {
				socket.close();
			} catch (IOException e) {
				// 테스트 종료 중이라 무시한다.
			}
		}
	}
}

export default {
  async fetch(request, env) {
    const requestUrl = new URL(request.url);

    if (!requestUrl.pathname.startsWith("/api/")) {
      return env.ASSETS.fetch(request);
    }

    const upstreamUrl = new URL(
      `${requestUrl.pathname}${requestUrl.search}`,
      env.API_ORIGIN
    );
    const upstreamRequest = new Request(upstreamUrl, request);
    upstreamRequest.headers.set("X-Coyote-Origin", env.ORIGIN_TOKEN);
    upstreamRequest.headers.set("X-Forwarded-Host", requestUrl.host);
    upstreamRequest.headers.set("X-Forwarded-Proto", "https");
    const clientIp = request.headers.get("CF-Connecting-IP");
    if (clientIp) {
      upstreamRequest.headers.set("X-Coyote-Client-IP", clientIp);
    } else {
      upstreamRequest.headers.delete("X-Coyote-Client-IP");
    }

    return fetch(upstreamRequest, { redirect: "manual" });
  },
};

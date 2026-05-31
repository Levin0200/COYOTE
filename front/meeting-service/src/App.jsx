function App() {
  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="w-96 bg-white rounded-2xl shadow-lg p-6">
        <h1 className="text-3xl font-bold mb-6">
          COYOTE
        </h1>

        <button
          className="w-full bg-blue-500 text-white py-3 rounded-xl hover:bg-blue-600"
        >
          모임 생성하기
        </button>
      </div>
    </div>
  )
}

export default App
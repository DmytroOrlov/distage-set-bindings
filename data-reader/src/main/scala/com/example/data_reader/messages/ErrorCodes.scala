package com.example.data_reader.messages

object ErrorCodes {

  object Internal {
    val UnexpectedError = 6000 // an unexpected error happened - HTTP: 500 Internal Server Error
    val InvalidRequest = 6001 // the received request is wrong - HTTP: 400 Bad Request
    val RequestTimeout = 6002 // the application has not completed the request on time - HTTP: 503 Service Unavailable
    val ServiceUnavailable = 6003 // the application is experiencing failures - HTTP: 503 Service Unavailable
  }

  object External {
    val ApplicationError = 6020
    val DatabaseError = 6021
  }

}

package com.example.data_reader.config

import java.net.URI

case class DataReaderConf(
    getHistory: GetHistoryConf,
    webServer: WebServerConfig,
    uri: URI,
)

final case class GetHistoryConf(days: Int, maxResults: Int)

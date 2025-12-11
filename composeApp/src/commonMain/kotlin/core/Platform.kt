package core

import io.ktor.client.HttpClient

// platform specific code, using "actual" implementations

// HttpClient is provided to ktor based on the platform
expect fun httpClient(): HttpClient
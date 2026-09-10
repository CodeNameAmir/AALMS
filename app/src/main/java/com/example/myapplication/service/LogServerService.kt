package com.example.myapplication.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapplication.LogApplication
import com.example.myapplication.MainActivity
import com.example.myapplication.data.model.LogEntry
import com.example.myapplication.data.model.ExternalLogEntry
import com.example.myapplication.data.model.DatabaseMode
import com.example.myapplication.data.CategoryCount
import com.example.myapplication.data.DayCount
import com.example.myapplication.data.LevelCount
import com.example.myapplication.data.LogWithCategory
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.NetworkInterface

@Serializable
data class WebSession(
    val userId: Long,
    val databaseMode: String = "LOCAL"
)

@Serializable
data class DatabaseModeRequest(val mode: String)

@Serializable
data class DatabaseModeResponse(
    val current_mode: String,
    val is_google_cloud_configured: Boolean
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val count: Int,
    val message: String
)

@Serializable
data class LogCreateRequest(
    val message: String,
    val level: String,
    val category_id: Long,
    val pinned: Boolean = false
)

@Serializable
data class CategoryCreateRequest(
    val name: String,
    val color: String
)

@Serializable
data class LogApiResponse(
    val id: Long,
    val user_id: String,
    val message: String,
    val level: String,
    val category_id: Long,
    val category_name: String,
    val category_color: String,
    val pinned: Boolean,
    val created_at: String,
    val updated_at: String,
    val attachment_name: String? = null,
    val attachment_path: String? = null,
    val attachment_size: Long? = null
)

@Serializable
data class LogsListResponse(
    val logs: List<LogApiResponse>,
    val total: Int
)

@Serializable
data class StatsApiResponse(
    val total_logs: Int,
    val total_categories: Int,
    val info: Int,
    val warning: Int,
    val error: Int,
    val critical: Int,
    val debug: Int,
    val by_category: List<CategoryCountApiResponse>,
    val by_day: List<DayCountApiResponse>
)

@Serializable
data class CategoryCountApiResponse(
    val category_name: String,
    val category_color: String,
    val count: Int
)

@Serializable
data class DayCountApiResponse(
    val day: String,
    val count: Int
)

@Serializable
data class ImportResponse(
    val imported: Int,
    val skipped: Int,
    val failed: Int
)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class SuccessResponse(val success: Boolean)

@Serializable
data class MeResponse(val username: String)


class LogServerService : Service() {

    private var server: EmbeddedServer<*, *>? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wifiLock: WifiManager.WifiLock? = null
    
    private val _logFlow = MutableSharedFlow<LogApiResponse>(extraBufferCapacity = 10)
    private val logFlow = _logFlow.asSharedFlow()

    companion object {
        const val ACTION_START = "START_SERVER"
        const val ACTION_STOP = "STOP_SERVER"
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "log_server_channel"
        var isRunning = false

        fun getLocalIpAddress(): String? {
            try {
                val en = java.net.NetworkInterface.getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val intf = en.nextElement()
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is java.net.Inet4Address) return inetAddress.hostAddress
                    }
                }
            } catch (ex: Exception) { ex.printStackTrace() }
            return null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, createNotification("Starting server..."))
                startServer()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startServer() {
        if (isRunning) return
        isRunning = true
        
        wifiLock = (getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LogServerLock")
        wifiLock?.acquire()

        val logApp = application as LogApplication
        val repository = logApp.repository
        val logDao = logApp.database.logDao()

        serviceScope.launch {
            try {
                server = embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            encodeDefaults = true
                            ignoreUnknownKeys = true
                            isLenient = true
                        })
                    }
                    install(SSE)
                    install(Sessions) {
                        cookie<WebSession>("WEB_SESSION") {
                            cookie.path = "/"
                            cookie.maxAgeInSeconds = 3600 * 24 * 7 // 1 week
                            cookie.httpOnly = true
                            cookie.secure = false
                        }
                    }
                    routing {
                        intercept(ApplicationCallPipeline.Plugins) {
                            val session = call.sessions.get<WebSession>()
                            val path = call.request.uri
                            
                            val isAuthPath = path.startsWith("/api/login") || 
                                            path.startsWith("/api/signup") || 
                                            path == "/login" ||
                                            path.startsWith("/static")
                            
                            if (session == null && !isAuthPath && path != "/") {
                                if (path.startsWith("/api")) {
                                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                                } else {
                                    call.respondRedirect("/login")
                                }
                                return@intercept finish()
                            }
                        }

                        get("/") {
                            val session = call.sessions.get<WebSession>()
                            if (session == null) {
                                call.respondRedirect("/login")
                            } else {
                                try {
                                    val html = assets.open("web/index.html").bufferedReader().use { it.readText() }
                                    call.respondText(html, ContentType.Text.Html)
                                } catch (e: Exception) {
                                    call.respondText("Error loading index.html: ${e.message}", status = HttpStatusCode.InternalServerError)
                                }
                            }
                        }
                        
                        get("/login") {
                            try {
                                val html = assets.open("web/login.html").bufferedReader().use { it.readText() }
                                call.respondText(html, ContentType.Text.Html)
                            } catch (e: Exception) {
                                call.respondText("Error loading login.html: ${e.message}", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        staticResources("/static", "web")

                        get("/uploads/{name}") {
                            val name = call.parameters["name"] ?: return@get
                            val file = File(filesDir, name)
                            if (file.exists()) {
                                val originalName = try {
                                    val parts = name.split("_")
                                    if (parts.size >= 3) name.substringAfter(parts[1] + "_") else name
                                } catch (e: Exception) { name }
                                
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, originalName).toString()
                                )
                                call.respondFile(file)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        }

                        route("/api") {
                            post("/login") {
                                 try {
                                     val params = call.receive<Map<String, String>>()
                                     val username = params["username"] ?: ""
                                     val password = params["password"] ?: ""
                                     
                                     val user = logDao.getUserByUsername(username)
                                     
                                     if (user != null && checkPassword(password, user.passwordHash)) {
                                         val userMode = try {
                                             repository.userManager.databaseMode.first().name
                                         } catch (e: Exception) {
                                             "LOCAL"
                                         }
                                         call.sessions.set(WebSession(userId = user.id, databaseMode = userMode))
                                         call.respond(SuccessResponse(true))
                                     } else {
                                         call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
                                     }
                                 } catch (e: Exception) {
                                     call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "unknown error"))
                                 }
                            }
                            post("/signup") {
                                try {
                                    val params = call.receive<Map<String, String>>()
                                    val username = params["username"] ?: ""
                                    val password = params["password"] ?: ""
                                    
                                    val existingUser = logDao.getUserByUsername(username)
                                    if (existingUser != null) {
                                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("User already exists"))
                                        return@post
                                    }
                                    val newUser = com.example.myapplication.data.model.User(username = username, passwordHash = hashPassword(password))
                                    val userId = logDao.insertUser(newUser)
                                    if (userId > 0) {
                                        call.sessions.set(WebSession(userId = userId, databaseMode = "LOCAL"))
                                        call.respond(SuccessResponse(true))
                                    } else call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Registration failed"))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "unknown error"))
                                }
                            }

                            authenticateSession {
                                fun getCurrentMode(session: WebSession): DatabaseMode {
                                    return try {
                                        DatabaseMode.valueOf(session.databaseMode)
                                    } catch (e: Exception) {
                                        DatabaseMode.LOCAL
                                    }
                                }

                                get("/me") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val user = logDao.getUserById(session.userId)
                                    call.respond(MeResponse(user?.username ?: "User #${session.userId}"))
                                }
                                get("/database-mode") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = session.databaseMode
                                    val isConfigured = com.example.myapplication.data.firebase.FirebaseManager.isReady()
                                    call.respond(DatabaseModeResponse(current_mode = mode, is_google_cloud_configured = isConfigured))
                                }
                                post("/database-mode") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val req = call.receive<DatabaseModeRequest>()
                                    val targetMode = try {
                                        DatabaseMode.valueOf(req.mode)
                                    } catch (e: Exception) {
                                        DatabaseMode.LOCAL
                                    }
                                    // Update session
                                    call.sessions.set(session.copy(databaseMode = targetMode.name))
                                    // Also update app's UserManager preference so syncs reflect properly
                                    try {
                                        repository.switchDatabaseMode(targetMode)
                                    } catch (e: Exception) { /* non-fatal */ }
                                    call.respond(DatabaseModeResponse(current_mode = targetMode.name, is_google_cloud_configured = com.example.myapplication.data.firebase.FirebaseManager.isReady()))
                                }
                                post("/sync") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val direction = call.request.queryParameters["direction"] ?: "local_to_cloud"
                                    try {
                                        val count = if (direction == "cloud_to_local") {
                                            repository.syncGoogleCloudToLocal(session.userId)
                                        } else {
                                            repository.syncLocalToGoogleCloud(session.userId)
                                        }
                                        call.respond(SyncResponse(success = true, count = count, message = "Successfully synced $count items"))
                                    } catch (e: Exception) {
                                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Sync failed"))
                                    }
                                }
                                post("/change-password") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val params = call.receive<Map<String, String>>()
                                    val current = params["current_password"] ?: ""
                                    val next = params["new_password"] ?: ""
                                    val user = logDao.getUserById(session.userId)
                                    if (user != null && checkPassword(current, user.passwordHash)) {
                                        val updatedUser = user.copy(passwordHash = hashPassword(next))
                                        logDao.updateUser(updatedUser)
                                        call.respond(SuccessResponse(true))
                                    } else call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid current password"))
                                }
                                post("/import") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val format = call.request.queryParameters["format"] ?: "json"
                                    val multipart = call.receiveMultipart()
                                    var imported = 0
                                    var skipped = 0
                                    var failed = 0
                                    
                                    multipart.forEachPart { part ->
                                        if (part is PartData.FileItem) {
                                            val bytes = part.streamProvider().readBytes()
                                            val content = String(bytes)
                                            try {
                                                val externalLogs = if (format == "json") {
                                                    val gson = Gson()
                                                    val type = object : com.google.gson.reflect.TypeToken<List<ExternalLogEntry>>() {}.type
                                                    gson.fromJson<List<ExternalLogEntry>>(content, type)
                                                } else if (format == "csv") {
                                                    com.example.myapplication.util.ImportExportUtils.importFromCsvContent(content)
                                                } else null
                                                
                                                if (externalLogs != null) {
                                                    val categories = repository.getAllCategoriesForMode(session.userId, mode).first()
                                                    val generalCat = categories.find { it.name.equals("General", ignoreCase = true) } 
                                                                    ?: categories.firstOrNull()
                                                    
                                                    externalLogs.forEach { l ->
                                                        try {
                                                            var catId = 0L
                                                            if (l.categoryId != null && categories.any { it.id == l.categoryId }) {
                                                                catId = l.categoryId
                                                            } else if (l.categoryName != null) {
                                                                catId = categories.find { it.name.equals(l.categoryName, ignoreCase = true) }?.id 
                                                                ?: run {
                                                                    val newCat = com.example.myapplication.data.model.Category(name = l.categoryName, color = l.categoryColor ?: "#3b82f6", userId = session.userId)
                                                                    repository.insertCategoryForMode(newCat, mode)
                                                                }
                                                            }
                                                            
                                                            if (catId == 0L) {
                                                                catId = generalCat?.id ?: 1L
                                                            }

                                                            val timestamp = com.example.myapplication.util.ImportExportUtils.parseTimestamp(l.createdAt)

                                                            val log = LogEntry(
                                                                id = 0L,
                                                                message = l.message,
                                                                level = l.level,
                                                                categoryId = catId,
                                                                userId = session.userId,
                                                                pinned = l.pinned,
                                                                createdAt = timestamp,
                                                                updatedAt = com.example.myapplication.util.ImportExportUtils.parseTimestamp(l.updatedAt),
                                                                attachmentName = l.attachmentName,
                                                                attachmentPath = l.attachmentPath,
                                                                attachmentSize = l.attachmentSize
                                                            )
                                                            
                                                            repository.insertLogForMode(log, mode)
                                                            imported++
                                                        } catch (e: Exception) { failed++ }
                                                    }
                                                }
                                            } catch (e: Exception) { failed++ }
                                        }
                                        part.dispose()
                                    }
                                    call.respond(ImportResponse(imported = imported, skipped = skipped, failed = failed))
                                }
                                get("/categories") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val cats = repository.getAllCategoriesForMode(session.userId, mode).first()
                                    call.respond(cats)
                                }
                                post("/categories") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val req = call.receive<CategoryCreateRequest>()
                                    val cat = com.example.myapplication.data.model.Category(name = req.name, color = req.color, userId = session.userId)
                                    repository.insertCategoryForMode(cat, mode)
                                    call.respond(SuccessResponse(true))
                                }
                                delete("/categories/{id}") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val id = call.parameters["id"]?.toLongOrNull() ?: return@delete
                                    val cat = com.example.myapplication.data.model.Category(id = id, name = "", userId = session.userId)
                                    repository.deleteCategoryForMode(cat, mode)
                                    call.respond(SuccessResponse(true))
                                }
                                get("/logs") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val q = call.request.queryParameters["q"] ?: ""
                                    val level = call.request.queryParameters["level"] ?: ""
                                    val catId = call.request.queryParameters["category_id"]?.toLongOrNull() ?: 0L
                                    val start = call.request.queryParameters["start"]?.toLongOrNull() ?: 0L
                                    val end = call.request.queryParameters["end"]?.toLongOrNull() ?: 0L
                                    
                                    val logs: List<LogWithCategory> = repository.getFilteredLogsForMode(
                                        userId = session.userId,
                                        mode = mode,
                                        search = q,
                                        level = level,
                                        categoryId = catId,
                                        startDate = start,
                                        endDate = end
                                    ).first()
                                    
                                    val logList = logs.map { 
                                            LogApiResponse(
                                                id = it.log.id,
                                                user_id = it.log.userId.toString(),
                                                message = it.log.message,
                                                level = it.log.level,
                                                category_id = it.log.categoryId,
                                                category_name = it.categoryName,
                                                category_color = it.categoryColor,
                                                pinned = it.log.pinned,
                                                created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'0000Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(it.log.createdAt)),
                                                updated_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'0000Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(it.log.updatedAt)),
                                                attachment_name = it.log.attachmentName,
                                                attachment_path = it.log.attachmentPath?.substringAfterLast("/"),
                                                attachment_size = it.log.attachmentSize
                                            )
                                        }
                                    call.respond(LogsListResponse(logs = logList, total = logList.size))
                                }
                                sse("/logs/stream") {
                                    val session = call.sessions.get<WebSession>()!!
                                    logFlow.collect { log ->
                                        if (log.user_id == session.userId.toString()) {
                                            send(ServerSentEvent(data = Json.encodeToString(log)))
                                        }
                                    }
                                }
                                post("/logs") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val req = call.receive<LogCreateRequest>()
                                    val log = LogEntry(message = req.message, level = req.level, categoryId = req.category_id, userId = session.userId, pinned = req.pinned)
                                    val id = repository.insertLogForMode(log, mode)
                                    
                                    val cat = repository.getCategoryByIdForMode(req.category_id, session.userId, mode)
                                    
                                    val apiResponse = LogApiResponse(
                                        id = id,
                                        user_id = session.userId.toString(),
                                        message = log.message,
                                        level = log.level,
                                        category_id = log.categoryId,
                                        category_name = cat?.name ?: "Unknown",
                                        category_color = cat?.color ?: "#808080",
                                        pinned = log.pinned,
                                        created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'0000Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(log.createdAt)),
                                        updated_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'0000Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(log.updatedAt))
                                    )
                                    _logFlow.emit(apiResponse)
                                    
                                    call.respond(mapOf("success" to true, "id" to id))
                                }
                                post("/logs/{id}/pin") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val id = call.parameters["id"]?.toLongOrNull() ?: return@post
                                    repository.togglePinForMode(id, session.userId, mode)
                                    call.respond(SuccessResponse(true))
                                }
                                delete("/logs/{id}") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val id = call.parameters["id"]?.toLongOrNull() ?: return@delete
                                    repository.deleteLogByIdForMode(id, session.userId, mode)
                                    call.respond(SuccessResponse(true))
                                }
                                post("/logs/{id}/attachments") {
                                     val session = call.sessions.get<WebSession>()!!
                                     val mode = getCurrentMode(session)
                                     val logId = call.parameters["id"]?.toLongOrNull() ?: return@post
                                     val multipart = call.receiveMultipart()
                                     var fileName = ""
                                     var filePath = ""
                                     var fileSize = 0L
                                     multipart.forEachPart { part ->
                                         if (part is PartData.FileItem) {
                                             fileName = part.originalFileName ?: "file"
                                             val file = File(filesDir, "attach_${System.currentTimeMillis()}_$fileName")
                                             part.streamProvider().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                                             filePath = file.absolutePath
                                             fileSize = file.length()
                                         }
                                         part.dispose()
                                     }
                                     val log = repository.getLogByIdForMode(logId, session.userId, mode)
                                     if (log != null && log.userId == session.userId) {
                                         val updatedLog = log.copy(attachmentName = fileName, attachmentPath = filePath, attachmentSize = fileSize)
                                         repository.updateLogForMode(updatedLog, mode)
                                         call.respond(SuccessResponse(true))
                                     } else call.respond(HttpStatusCode.NotFound, ErrorResponse("Log not found"))
                                }
                                get("/export") {
                                     val session = call.sessions.get<WebSession>()!!
                                     val mode = getCurrentMode(session)
                                     val format = call.request.queryParameters["format"] ?: "json"
                                     val logs = repository.getFilteredLogsForMode(session.userId, mode).first()
                                     
                                     if (format == "csv") {
                                         val csv = StringBuilder("id,level,category,pinned,message,attachment,attachment_size,created_at,updated_at\n")
                                         val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                                             timeZone = java.util.TimeZone.getTimeZone("UTC")
                                         }
                                         logs.forEach { 
                                             val msg = it.log.message.replace("\"", "\"\"")
                                             val cat = it.categoryName.replace("\"", "\"\"")
                                             val att = (it.log.attachmentName ?: "").replace("\"", "\"\"")
                                             val created = sdf.format(java.util.Date(it.log.createdAt))
                                             val updated = sdf.format(java.util.Date(it.log.updatedAt))
                                             csv.append("${it.log.id},${it.log.level},\"$cat\",${it.log.pinned.toString().uppercase()},\"$msg\",\"$att\",${it.log.attachmentSize ?: 0},$created,$updated\n")
                                         }
                                         call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "export_${System.currentTimeMillis()}.csv").toString())
                                         call.respondText(csv.toString(), ContentType.Text.CSV)
                                     } else {
                                         val externalLogs = logs.map { 
                                             with(com.example.myapplication.util.ImportExportUtils) { it.toExternal() } 
                                         }
                                         call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "export_${System.currentTimeMillis()}.json").toString())
                                         call.respondText(com.example.myapplication.util.ImportExportUtils.gson.toJson(externalLogs), ContentType.Application.Json)
                                     }
                                }
                                post("/logout") {
                                    call.sessions.clear<WebSession>()
                                    call.respond(SuccessResponse(true))
                                }
                                get("/stats") {
                                    val session = call.sessions.get<WebSession>()!!
                                    val mode = getCurrentMode(session)
                                    val total = repository.getTotalLogCountForMode(session.userId, mode).first()
                                    val levels = repository.getLogCountsByLevelForMode(session.userId, mode).first()
                                    val cats = repository.getLogCountsByCategoryForMode(session.userId, mode).first()
                                    val days = repository.getLogCountsByDayForMode(session.userId, mode).first()
                                    val response = StatsApiResponse(
                                        total_logs = total,
                                        total_categories = cats.size,
                                        info = levels.find { it.level == "INFO" }?.count ?: 0,
                                        warning = levels.find { it.level == "WARNING" }?.count ?: 0,
                                        error = levels.find { it.level == "ERROR" }?.count ?: 0,
                                        critical = levels.find { it.level == "CRITICAL" }?.count ?: 0,
                                        debug = levels.find { it.level == "DEBUG" }?.count ?: 0,
                                        by_category = cats.map { CategoryCountApiResponse(it.categoryName, it.categoryColor, it.count) },
                                        by_day = days.map { DayCountApiResponse(it.day, it.count) }
                                    )
                                    call.respond(response)
                                }
                            }
                        }
                    }
                }
                server?.start(wait = false)
                val ip = getLocalIpAddress() ?: "Unknown IP"
                updateNotification("Server running at http://$ip:8080")
            } catch (e: Exception) {
                isRunning = false
                wifiLock?.release()
                updateNotification("Server error: ${e.message}")
            }
        }
    }

    private fun hashPassword(password: String): String = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt())
    private fun checkPassword(password: String, hashed: String): Boolean = try { org.mindrot.jbcrypt.BCrypt.checkpw(password, hashed) } catch (e: Exception) { password == hashed }

    private fun Route.authenticateSession(callback: Route.() -> Unit) {
        callback()
    }

    private fun createNotification(text: String): Notification {
        val stopIntent = Intent(this, LogServerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Log Manager Server")
            .setContentText(text)
            .setSmallIcon(com.example.myapplication.R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Log Server Status", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        server?.stop(1000, 2000)
        wifiLock?.release()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

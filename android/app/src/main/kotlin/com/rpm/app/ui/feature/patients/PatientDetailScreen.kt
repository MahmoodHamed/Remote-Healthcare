package com.rpm.app.ui.feature.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rpm.app.data.remote.dto.PatientDetailDto
import com.rpm.app.data.remote.dto.VitalRecordDto
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    userRole: String?,
    userId: String?,
    onBack: () -> Unit,
    onOpenChat: (conversationId: String) -> Unit,
    onOpenAlerts: (patientId: String) -> Unit,
    onOpenLiveMonitor: ((patientId: String) -> Unit)? = null,
    onOpenDeviceManagement: (() -> Unit)? = null,
    viewModel: PatientDetailViewModel = hiltViewModel(),
) {
    val uiState     by viewModel.uiState.collectAsState()
    val patient     = uiState.patient
    val vitalsTitle = vitalsSectionTitle(userRole, patient?.fullName)
    val showBack    = userRole != "Patient"

    val canChat = userRole == "Doctor" ||
        ((userRole == "Patient" || userRole == "Relative") && patient?.doctor != null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (userRole) {
                            "Patient" -> "My Health"
                            else      -> patient?.fullName ?: "Patient Detail"
                        },
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    // Refresh
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    // Alerts
                    IconButton(onClick = { patient?.let { onOpenAlerts(it.userId) } }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                    }
                    // Live Monitor (for Doctor and all roles that can monitor)
                    if (userRole == "Doctor" || userRole == "Relative") {
                        patient?.let { p ->
                            IconButton(onClick = { onOpenLiveMonitor?.invoke(p.userId) }) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = "Live Monitor", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    // Chat (top-bar shortcut for Doctor)
                    if (userRole == "Doctor") {
                        IconButton(
                            onClick  = { viewModel.startChat(userId, userRole, onOpenChat) },
                            enabled  = !uiState.isOpeningChat && patient != null,
                        ) {
                            if (uiState.isOpeningChat) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat")
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null && patient == null -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                patient != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // ── Vitals card ───────────────────────────────────
                        VitalsDetailCard(
                            title  = vitalsTitle,
                            live   = uiState.realtimeVitals,
                            latest = patient.latestVitals,
                            full   = uiState.latestVitals,
                        )

                        // ── Vitals History ────────────────────────────────
                        VitalsHistorySection(
                            userRole       = userRole,
                            history        = uiState.vitalsHistory,
                            isLoading      = uiState.isLoadingHistory,
                        )

                        // ── Profile card ──────────────────────────────────
                        ProfileSection(patient, userRole)

                        // ── Medical info ──────────────────────────────────
                        MedicalInfoSection(patient, userRole)

                        // ── Doctor & chat ─────────────────────────────────
                        if (patient.doctor != null || canChat) {
                            DoctorSection(
                                patient       = patient,
                                userRole      = userRole,
                                userId        = userId,
                                isOpeningChat = uiState.isOpeningChat,
                                onOpenChat    = { viewModel.startChat(userId, userRole, onOpenChat) },
                            )
                        }

                        // ── Live Monitor button ───────────────────────────
                        if (onOpenLiveMonitor != null) {
                            LiveMonitorBannerButton(onClick = { onOpenLiveMonitor(patient.userId) })
                        }

                        // ── My Watch (patient-only shortcut) ───────────────
                        if (userRole == "Patient") {
                            WatchLinkSection(onOpenDeviceManagement = onOpenDeviceManagement)
                        }

                        uiState.error?.let { msg ->
                            Text(msg, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ── Sections ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(patient: PatientDetailDto, userRole: String?) {
    DetailCard(
        title = when (userRole) {
            "Patient"  -> "My Profile"
            "Relative" -> "Family Member"
            else       -> "Patient Profile"
        },
        icon = Icons.Default.AccountCircle,
    ) {
        InfoRow(Icons.Default.Person, "Name", patient.fullName)
        patient.email?.let { InfoRow(Icons.Default.Email, "Email", it) }
        patient.phone?.let { InfoRow(Icons.Default.Phone, "Phone", it) }
        patient.dateOfBirth?.let { dob ->
            val formatted = formatDateOfBirth(dob)
            InfoRow(Icons.Default.CalendarMonth, "Date of Birth", formatted)
        }
        patient.bloodType?.let {
            InfoRow(Icons.Default.Bloodtype, "Blood Type", it)
        }
        if (patient.weightKg != null || patient.heightCm != null) {
            if (patient.weightKg != null && patient.heightCm != null) {
                val bmi = patient.weightKg / ((patient.heightCm / 100f) * (patient.heightCm / 100f))
                InfoRow(Icons.Default.Scale, "Weight", "%.1f kg".format(patient.weightKg))
                InfoRow(Icons.Default.Height, "Height", "%.0f cm".format(patient.heightCm))
                InfoRow(
                    icon  = Icons.Default.Analytics,
                    label = "BMI",
                    value = "%.1f — %s".format(bmi, bmiCategory(bmi)),
                )
            } else {
                patient.weightKg?.let { InfoRow(Icons.Default.Scale, "Weight", "%.1f kg".format(it)) }
                patient.heightCm?.let { InfoRow(Icons.Default.Height, "Height", "%.0f cm".format(it)) }
            }
        }
        patient.emergencyContactPhone?.let {
            InfoRow(Icons.Default.ContactEmergency, "Emergency Contact", it)
        }
    }
}

@Composable
private fun MedicalInfoSection(patient: PatientDetailDto, userRole: String?) {
    val hasMedical = patient.chronicDiseases.isNotEmpty() ||
        patient.allergies.isNotEmpty() ||
        patient.currentMedications.isNotEmpty()
    if (!hasMedical) return

    DetailCard(title = "Medical Information", icon = Icons.Default.MedicalServices) {
        if (patient.chronicDiseases.isNotEmpty()) {
            TagGroup(
                label          = "Chronic Diseases",
                items          = patient.chronicDiseases,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        if (patient.allergies.isNotEmpty()) {
            TagGroup(
                label          = "Allergies",
                items          = patient.allergies,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        if (patient.currentMedications.isNotEmpty()) {
            TagGroup(
                label          = "Current Medications",
                items          = patient.currentMedications,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun DoctorSection(
    patient: PatientDetailDto,
    userRole: String?,
    userId: String?,
    isOpeningChat: Boolean,
    onOpenChat: () -> Unit,
) {
    DetailCard(title = "Attending Doctor", icon = Icons.Default.LocalHospital) {
        patient.doctor?.let { doc ->
            InfoRow(Icons.Default.Person, "Name", doc.fullName)
            doc.specialization?.let { InfoRow(Icons.Default.WorkspacePremium, "Specialization", it) }
        } ?: run {
            Text(
                "No doctor assigned yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Chat button available for all roles (if doctor is assigned)
        val chatLabel = when (userRole) {
            "Doctor"   -> "Chat with Patient"
            "Patient"  -> "Chat with My Doctor"
            "Relative" -> "Chat with Doctor"
            else       -> "Open Chat"
        }
        if (patient.doctor != null) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onOpenChat,
                enabled  = !isOpeningChat,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
            ) {
                if (isOpeningChat) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(chatLabel)
                }
            }
        }
    }
}

// ── Shared Components ──────────────────────────────────────────────────────

@Composable
private fun DetailCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style      = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color      = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$label:",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            value,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagGroup(
    label: String,
    items: List<String>,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                ) {
                    Text(
                        item,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = contentColor,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

// ── Vitals History Section ─────────────────────────────────────────────────

@Composable
private fun VitalsHistorySection(
    userRole: String?,
    history: List<VitalRecordDto>,
    isLoading: Boolean,
) {
    val title = when (userRole) {
        "Patient"  -> "My Readings — Last 7 Days"
        "Relative" -> "Readings — Last 7 Days"
        else       -> "Vital Readings — Last 7 Days"
    }

    DetailCard(title = title, icon = Icons.Default.History) {
        when {
            isLoading -> {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }
            history.isEmpty() -> {
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    modifier            = Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No readings recorded in the last 7 days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    history.forEachIndexed { index, record ->
                        VitalRecordRow(record = record, index = index + 1)
                        if (index < history.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Showing ${history.size} reading${if (history.size != 1) "s" else ""}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalRecordRow(record: VitalRecordDto, index: Int) {
    var expanded by remember { mutableStateOf(index == 1) } // first row expanded by default

    Column {
        // ── Header row (always visible) ───────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Index badge
            Surface(
                shape  = RoundedCornerShape(8.dp),
                color  = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    formatVitalTimestamp(record.recordedAt),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                )
                // Inline summary: HR • SpO₂ • Temp
                val summary = buildString {
                    record.heartRateBpm?.let { append("HR ${it.toInt()}") }
                    record.spO2Percent?.let {
                        if (isNotEmpty()) append("  •  ")
                        append("SpO₂ ${it.toInt()}%")
                    }
                    (record.skinTemperatureC ?: record.temperatureC)?.let {
                        if (isNotEmpty()) append("  •  ")
                        append("Skin %.1f°C".format(it))
                    }
                    record.stressScore?.let {
                        if (isNotEmpty()) append("  •  ")
                        append("Stress ${it.toInt()}")
                    }
                    if (isEmpty() && record.stepsCount != null) {
                        append("${record.stepsCount} steps")
                    }
                }
                if (summary.isNotEmpty()) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Fall alert
                if (record.fallDetected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(3.dp))
                        Text("Fall detected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Watch status chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (SupportedVitals.isWearing(record)) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (SupportedVitals.isWearing(record)) Icons.Default.Watch else Icons.Default.WatchOff,
                        contentDescription = null,
                        tint     = if (SupportedVitals.isWearing(record)) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Expand toggle
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Expanded detail ───────────────────────────────────────────────
        if (expanded) {
            Surface(
                shape  = RoundedCornerShape(12.dp),
                color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            ) {
                Column(
                    modifier            = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SupportedVitals.historyRows(record).forEach { (label, value) ->
                        val status = when (label) {
                            "Heart Rate" -> record.heartRateBpm?.let { vitalStatus(it, 40f..100f) } ?: VitalHistoryStatus.Normal
                            "SpO₂"       -> record.spO2Percent?.let { vitalStatus(it, 95f..100f) } ?: VitalHistoryStatus.Normal
                            "Stress"     -> record.stressScore?.let {
                                if (it < 40f) VitalHistoryStatus.Normal
                                else if (it < 70f) VitalHistoryStatus.Warning
                                else VitalHistoryStatus.Critical
                            } ?: VitalHistoryStatus.Normal
                            else         -> VitalHistoryStatus.Normal
                        }
                        HistoryVitalRow(label, value, status)
                    }
                }
            }
        }
    }
}

private enum class VitalHistoryStatus { Normal, Warning, Critical }

private fun vitalStatus(value: Float, normalRange: ClosedFloatingPointRange<Float>): VitalHistoryStatus =
    if (value in normalRange) VitalHistoryStatus.Normal else VitalHistoryStatus.Warning

@Composable
private fun HistoryVitalRow(label: String, value: String, status: VitalHistoryStatus) {
    val dotColor = when (status) {
        VitalHistoryStatus.Normal   -> Color(0xFF2E7D32)
        VitalHistoryStatus.Warning  -> Color(0xFFF57C00)
        VitalHistoryStatus.Critical -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint     = dotColor,
                modifier = Modifier.size(8.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────
private fun formatDateOfBirth(dob: String): String = runCatching {
    val date = LocalDate.parse(dob)
    val age  = Period.between(date, LocalDate.now()).years
    val fmt  = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    "$fmt ($age years)"
}.getOrDefault(dob)

// ── Live Monitor Banner Button ─────────────────────────────────────────────

@Composable
private fun LiveMonitorBannerButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Default.MonitorHeart,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Live Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Real-time · all 14 sensors · live history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun bmiCategory(bmi: Float) = when {
    bmi < 18.5f -> "Underweight"
    bmi < 25f   -> "Normal"
    bmi < 30f   -> "Overweight"
    else        -> "Obese"
}

// ── WatchLinkSection ───────────────────────────────────────────────────────

@Composable
private fun WatchLinkSection(onOpenDeviceManagement: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Watch,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Smartwatch",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Link your Galaxy Watch to send health data automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            OutlinedButton(
                onClick  = { onOpenDeviceManagement?.invoke() },
                enabled  = onOpenDeviceManagement != null,
            ) {
                Text("Setup")
            }
        }
    }
}

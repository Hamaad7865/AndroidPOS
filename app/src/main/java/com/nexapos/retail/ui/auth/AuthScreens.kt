package com.nexapos.retail.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.entity.StaffRole
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.data.security.PinManager
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.NexaLogo
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.components.isPortrait
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bone = Color(0xFFF4ECDD)

// ---------------------------------------------------------------------------
// Splash
// ---------------------------------------------------------------------------

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val c = PosTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1800, easing = LinearEasing))
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .background(Brush.radialGradient(listOf(c.amber.copy(alpha = 0.32f), Color.Transparent), center = Offset(380f, 260f), radius = 760f))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NexaLogo(size = 72.dp)
                Column {
                    Text("NexaPOS", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em, color = Bone)
                    Text("RETAIL · WORKSHOP PRECISION", fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 0.18.em, color = c.amber)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("POS", "Inventory", "Accounting", "Reports").forEach {
                    Box(Modifier.clip(CircleShape).border(1.dp, Bone.copy(alpha = 0.25f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Bone.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(160.dp).height(3.dp).clip(CircleShape).background(Bone.copy(alpha = 0.15f))) {
                    Box(Modifier.fillMaxWidth(progress.value.coerceIn(0.04f, 1f)).height(3.dp).clip(CircleShape).background(c.amber))
                }
                val step = (progress.value * 19).toInt().coerceIn(0, 19)
                Text("SYNCING LEDGER · $step OF 19", fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 0.12.em, color = Bone.copy(alpha = 0.6f))
            }
        }
        Box(Modifier.fillMaxSize().padding(bottom = 28.dp), contentAlignment = Alignment.BottomCenter) {
            Text("v 2.4 · BUILD 0526", fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 0.2.em, color = Bone.copy(alpha = 0.5f))
        }
    }
}

// ---------------------------------------------------------------------------
// Login
// ---------------------------------------------------------------------------

@Composable
fun LoginScreen(
    verifyPin: suspend (String) -> Boolean,
    lockoutRemainingMs: () -> Long,
    onAuthenticated: () -> Unit,
    onCreate: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val businessConfigured = remember { BusinessProfile.isConfigured(context) }
    val portrait = isPortrait()
    val rootMod = Modifier.fillMaxSize().background(c.bg).systemBarsPadding()
    if (portrait) {
        Box(rootMod, contentAlignment = Alignment.Center) {
            LoginForm(businessConfigured, verifyPin, lockoutRemainingMs, onAuthenticated, onCreate)
        }
        return
    }
    Row(rootMod) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            LoginForm(businessConfigured, verifyPin, lockoutRemainingMs, onAuthenticated, onCreate)
        }
        Column(
            Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .background(c.ink)
                .background(Brush.radialGradient(listOf(c.amber.copy(alpha = 0.4f), Color.Transparent), center = Offset(900f, 0f), radius = 800f))
                .padding(40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InkPill("EN · Rs MUR")
                InkPill("Counter")
            }
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Text("NEXAPOS · RETAIL", fontFamily = JetBrainsMono, fontSize = 13.sp, letterSpacing = 0.18.em, fontWeight = FontWeight.SemiBold, color = c.amber)
                Spacer(Modifier.height(14.dp))
                Text("The till you can hear tick when it's fast.", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.025).em, lineHeight = 44.sp, color = Bone)
                Spacer(Modifier.height(14.dp))
                Text(
                    "Sub-second product lookup, an offline-first ledger, and reports your accountant will actually accept. Built for shop counters.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Bone.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    FeaturePill("Offline-first")
                    FeaturePill("Encrypted")
                    FeaturePill("No subscription")
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text("© NEXAPOS · MU", fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 0.14.em, color = Bone.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun LoginForm(
    businessConfigured: Boolean,
    verifyPin: suspend (String) -> Boolean,
    lockoutRemainingMs: () -> Long,
    onAuthenticated: () -> Unit,
    onCreate: () -> Unit,
) {
    val c = PosTheme.colors
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    // Remaining lockout in seconds, refreshed every second while > 0.
    var lockoutSecs by remember { mutableLongStateOf(0L) }

    // Poll the lockout countdown every second while the account is locked.
    LaunchedEffect(Unit) {
        while (true) {
            lockoutSecs = lockoutRemainingMs() / 1_000L
            if (lockoutSecs <= 0L) {
                delay(500L) // light idle poll — re-check after half a second
            } else {
                delay(1_000L)
            }
        }
    }

    val isLocked = lockoutSecs > 0L

    fun submit() {
        val notReady = checking || pin.isBlank() || isLocked
        if (notReady) return
        error = false
        checking = true
        scope.launch {
            val ok = verifyPin(pin.trim())
            checking = false
            if (ok) {
                onAuthenticated()
            } else {
                error = true
                // Re-read lockout immediately in case this failure triggered a lockout.
                lockoutSecs = lockoutRemainingMs() / 1_000L
            }
        }
    }

    Column(
        Modifier.widthIn(max = 380.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NexaLogo(size = 32.dp)
            Text("NexaPOS", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
        }
        Column {
            Text("WELCOME BACK", fontSize = 11.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold, color = c.muted)
            Spacer(Modifier.height(6.dp))
            Text("Sign in to your counter.", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em, lineHeight = 36.sp, color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text("Use your shop credentials to open the till.", fontSize = 14.sp, color = c.muted)
        }
        EditableField("Staff PIN", pin, { pin = it }, Modifier.fillMaxWidth(), mono = true, number = true, placeholder = "Enter your PIN")
        when {
            isLocked -> Text("Too many attempts, try again in ${lockoutSecs}s.", fontSize = 12.sp, color = c.crimson)
            error -> Text("Incorrect PIN. Try again.", fontSize = 12.sp, color = c.crimson)
        }
        WideBtn(
            when {
                isLocked -> "Locked — wait ${lockoutSecs}s"
                checking -> "Checking…"
                else -> "Sign in · open till"
            },
            primary = true,
            Modifier.fillMaxWidth(),
            icon = PosIcons.arrowR,
            onClick = { submit() },
        )
        if (!businessConfigured) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("First time? ", fontSize = 12.sp, color = c.muted)
                Text("Set up the shop", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.amber, modifier = Modifier.clickable { onCreate() })
            }
        }
    }
}

@Composable
private fun InkPill(text: String) {
    val c = PosTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Bone.copy(alpha = 0.18f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Bone)
        PosIcon(PosIcons.chevD, tint = Bone, size = 12.dp)
    }
}

@Composable
private fun FeaturePill(label: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .border(1.dp, Bone.copy(alpha = 0.25f), RoundedCornerShape(99.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Bone)
    }
}

// ---------------------------------------------------------------------------
// Business setup — single-page form. Saves to BusinessProfile and finishes.
// ---------------------------------------------------------------------------

@Composable
fun BusinessSetupScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    var name by remember { mutableStateOf(BusinessProfile.name(context).takeUnless { it == BusinessProfile.DEFAULT_NAME } ?: "") }
    var address by remember { mutableStateOf(BusinessProfile.address(context)) }
    var brn by remember { mutableStateOf(BusinessProfile.brn(context)) }
    var vat by remember { mutableStateOf(BusinessProfile.vat(context)) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val pinValid = pin.length in 4..8 && pin.all { char -> char.isDigit() }
    // PIN is required: blank is no longer allowed.
    val pinOk = pinValid && pin == confirmPin
    val canSave = name.isNotBlank() && pinOk
    val scope = rememberCoroutineScope()
    // Guards against a double-tap creating two "Owner" admins before the first
    // addStaff finishes (the activeStaff()==empty check is otherwise racy).
    var saving by remember { mutableStateOf(false) }

    fun save() {
        if (saving) return
        saving = true
        BusinessProfile.setProfile(context, name, address, brn, vat)
        PinManager.setPin(context, pin.trim())
        // The person doing first-run setup is the owner: record them as the
        // shop's admin so roles work from the very first sign-in.
        val container = (context.applicationContext as PosApplication).container
        scope.launch {
            try {
                val owner =
                    withContext(Dispatchers.Default) {
                        val repo = container.staffRepository
                        if (repo.activeStaff().isEmpty()) {
                            repo.addStaff(name = "Owner", pin = pin.trim(), role = StaffRole.ADMIN)
                        } else {
                            // Setup re-ran on a shop that already has staff: don't create
                            // a duplicate owner — sign in whoever this PIN belongs to.
                            repo.findByPin(pin.trim())
                        }
                    }
                if (owner != null) container.session.login(owner)
                onDone()
            } finally {
                saving = false
            }
        }
    }

    Row(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Column(
            Modifier.width(280.dp).fillMaxHeight().background(c.ink).padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NexaLogo(size = 28.dp)
                    Text("NexaPOS", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Bone)
                }
                Spacer(Modifier.height(30.dp))
                Text("FIRST-RUN SETUP", fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold, color = c.amber)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Let's set up the shop.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).em,
                    lineHeight = 28.sp,
                    color = Bone,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Just two essentials and you're ready to sell. " +
                        "Add staff, accounts, VAT rates and printers any time in Settings.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Bone.copy(alpha = 0.7f),
                )
            }
            Text("You can change everything later in Settings.", fontSize = 12.sp, color = Bone.copy(alpha = 0.6f))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 28.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "BUSINESS PROFILE",
                        fontSize = 11.sp,
                        letterSpacing = 0.14.em,
                        fontWeight = FontWeight.SemiBold,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tell us who's selling",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.015).em,
                        color = c.ink,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    com.nexapos.retail.ui.components.FormSection("Business identity") {
                        EditableField(
                            label = "Business name *",
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "e.g. QUINCAILLERIE RB TRADING",
                        )
                        Spacer(Modifier.height(12.dp))
                        EditableField(
                            label = "Address",
                            value = address,
                            onValueChange = { address = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Royal Rd, Curepipe",
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            EditableField(
                                label = "BRN",
                                value = brn,
                                onValueChange = { brn = it },
                                modifier = Modifier.weight(1f),
                                mono = true,
                                placeholder = "e.g. C20177445",
                            )
                            EditableField(
                                label = "VAT number",
                                value = vat,
                                onValueChange = { vat = it },
                                modifier = Modifier.weight(1f),
                                mono = true,
                                placeholder = "e.g. VAT20188822",
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.amberTint).border(1.dp, c.amberSoft, RoundedCornerShape(10.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PosIcon(PosIcons.bell, tint = c.amberPress, size = 16.dp)
                            Text(
                                "These appear in the Settings header, the POS app bar and at the top of every printed receipt.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = c.graphite,
                            )
                        }
                    }
                    com.nexapos.retail.ui.components.FormSection("Staff PIN (required)") {
                        Text(
                            "Used to unlock the till each time the app opens. A PIN is required to continue.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = c.graphite,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            EditableField(
                                label = "PIN (4–8 digits) *",
                                value = pin,
                                onValueChange = { pin = it },
                                modifier = Modifier.weight(1f),
                                mono = true,
                                number = true,
                                placeholder = "e.g. 4827",
                            )
                            EditableField(
                                label = "Confirm PIN *",
                                value = confirmPin,
                                onValueChange = { confirmPin = it },
                                modifier = Modifier.weight(1f),
                                mono = true,
                                number = true,
                                placeholder = "repeat",
                            )
                        }
                        if (pin.isNotBlank() && !pinValid) {
                            Spacer(Modifier.height(6.dp))
                            Text("PIN must be 4–8 digits.", fontSize = 12.sp, color = c.crimson)
                        } else if (pin.isNotBlank() && pin != confirmPin) {
                            Spacer(Modifier.height(6.dp))
                            Text("PINs don't match.", fontSize = 12.sp, color = c.crimson)
                        }
                    }
                    com.nexapos.retail.ui.components.FormSection("What's set automatically") {
                        SetupNote("VAT rate is 15% (Mauritius retail standard). Per-product overrides come later.")
                        Spacer(Modifier.height(6.dp))
                        SetupNote("Currency is Rupees (Rs). All money is stored to the cent for accuracy.")
                        Spacer(Modifier.height(6.dp))
                        SetupNote("Tickets start at S-00010 and increment by one per completed sale.")
                        Spacer(Modifier.height(6.dp))
                        SetupNote("Your PIN signs you in as the admin. Add cashiers in Settings → Staff & roles.")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WideBtn("‹ Back", primary = false, Modifier.width(120.dp), onClick = onBack)
                        WideBtn(
                            label = "Save & open the till",
                            primary = true,
                            modifier = Modifier.width(280.dp),
                            icon = PosIcons.arrowR,
                            onClick = { if (canSave) save() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupNote(text: String) {
    val c = PosTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PosIcon(PosIcons.check, tint = c.emerald, size = 14.dp)
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = c.graphite)
    }
}

package com.example.model

import com.squareup.moshi.JsonClass

enum class InputType(val displayName: String, val category: String) {
    // Face Buttons
    BUTTON_A("A Button", "Face Buttons"),
    BUTTON_B("B Button", "Face Buttons"),
    BUTTON_X("X Button", "Face Buttons"),
    BUTTON_Y("Y Button", "Face Buttons"),

    // D-Pad
    DPAD_UP("D-Pad Up", "D-Pad"),
    DPAD_DOWN("D-Pad Down", "D-Pad"),
    DPAD_LEFT("D-Pad Left", "D-Pad"),
    DPAD_RIGHT("D-Pad Right", "D-Pad"),
    DPAD("Directional Pad (Full)", "D-Pad"),

    // Analog Sticks
    STICK_L("Left Stick (LS)", "Joysticks"),
    STICK_R("Right Stick (RS)", "Joysticks"),
    STICK_L_CLICK("Left Stick Click (L3)", "Joysticks"),
    STICK_R_CLICK("Right Stick Click (R3)", "Joysticks"),

    // Shoulder Buttons
    BUMPER_L("Left Bumper (LB)", "Shoulder Buttons"),
    BUMPER_R("Right Bumper (RB)", "Shoulder Buttons"),

    // Triggers
    TRIGGER_L("Left Trigger (LT)", "Triggers"),
    TRIGGER_R("Right Trigger (RT)", "Triggers"),

    // System Buttons
    BUTTON_MENU("Start / Menu", "System Buttons"),
    BUTTON_VIEW("Back / View", "System Buttons"),
    BUTTON_XBOX("Home / Xbox Button", "System Buttons"),

    // Additional Buttons (some controllers)
    BUTTON_SHARE("Share Button", "Additional Buttons"),
    BUTTON_SCREENSHOT("Screenshot Button", "Additional Buttons"),
    BUTTON_M1("Macro Button (M1)", "Additional Buttons"),
    BUTTON_M2("Macro Button (M2)", "Additional Buttons"),
    BUTTON_M3("Macro Button (M3)", "Additional Buttons"),
    BUTTON_M4("Macro Button (M4)", "Additional Buttons"),
    BUTTON_PROFILE("Profile Button", "Additional Buttons"),
    BUTTON_TURBO("Turbo Button", "Additional Buttons")
}

@JsonClass(generateAdapter = true)
data class ControllerInput(
    val id: String,
    val type: InputType,
    val pctX: Float, // horizontal pos: 0.0 to 1.0
    val pctY: Float, // vertical pos: 0.0 to 1.0
    val actionName: String, // E.g. "Jump", "Gas", "Steer"
    val scale: Float = 1.0f
)

object LayoutPresets {
    fun getStandardLayout(): List<ControllerInput> {
        return listOf(
            ControllerInput("STICK_L", InputType.STICK_L, 0.30f, 0.42f, "Move / L-Stick"),
            ControllerInput("DPAD", InputType.DPAD, 0.40f, 0.65f, "D-Pad Navigation"),
            ControllerInput("STICK_R", InputType.STICK_R, 0.58f, 0.65f, "Camera Look / R-Stick"),
            
            ControllerInput("BUTTON_Y", InputType.BUTTON_Y, 0.70f, 0.28f, "Jump / Heavy Attack"),
            ControllerInput("BUTTON_X", InputType.BUTTON_X, 0.64f, 0.42f, "Interact / Reload"),
            ControllerInput("BUTTON_B", InputType.BUTTON_B, 0.76f, 0.42f, "Crouch / Dodge"),
            ControllerInput("BUTTON_A", InputType.BUTTON_A, 0.70f, 0.56f, "Select / Action"),
            
            ControllerInput("BUTTON_XBOX", InputType.BUTTON_XBOX, 0.50f, 0.30f, "Xbox Guide"),
            ControllerInput("BUTTON_VIEW", InputType.BUTTON_VIEW, 0.43f, 0.43f, "Scoreboard"),
            ControllerInput("BUTTON_MENU", InputType.BUTTON_MENU, 0.57f, 0.43f, "Pause Menu"),
            
            ControllerInput("BUMPER_L", InputType.BUMPER_L, 0.28f, 0.16f, "Weapon Prev (LB)"),
            ControllerInput("BUMPER_R", InputType.BUMPER_R, 0.72f, 0.16f, "Weapon Next (RB)"),
            ControllerInput("TRIGGER_L", InputType.TRIGGER_L, 0.18f, 0.08f, "Aim Down Sights (LT)"),
            ControllerInput("TRIGGER_R", InputType.TRIGGER_R, 0.82f, 0.08f, "Fire Weapon (RT)")
        )
    }

    fun getRacingLayout(): List<ControllerInput> {
        return listOf(
            ControllerInput("STICK_L", InputType.STICK_L, 0.24f, 0.48f, "Steer Left/Right", 1.35f),
            ControllerInput("TRIGGER_L", InputType.TRIGGER_L, 0.12f, 0.18f, "Brake / Reverse (LT)", 1.3f),
            ControllerInput("TRIGGER_R", InputType.TRIGGER_R, 0.88f, 0.18f, "Gas / Throttle (RT)", 1.30f),
            
            ControllerInput("BUTTON_Y", InputType.BUTTON_Y, 0.75f, 0.25f, "Camera Toggle"),
            ControllerInput("BUTTON_B", InputType.BUTTON_B, 0.82f, 0.44f, "Gear Up (+)"),
            ControllerInput("BUTTON_X", InputType.BUTTON_X, 0.68f, 0.44f, "Gear Down (-)"),
            ControllerInput("BUTTON_A", InputType.BUTTON_A, 0.75f, 0.63f, "E-Brake / Drift", 1.25f),
            
            ControllerInput("BUTTON_XBOX", InputType.BUTTON_XBOX, 0.50f, 0.30f, "Xbox Home"),
            ControllerInput("BUTTON_MENU", InputType.BUTTON_MENU, 0.57f, 0.43f, "Settings Menu")
        )
    }

    fun getSimulationLayout(): List<ControllerInput> {
        return listOf(
            ControllerInput("STICK_L", InputType.STICK_L, 0.28f, 0.38f, "Pitch & Roll"),
            ControllerInput("DPAD", InputType.DPAD, 0.38f, 0.65f, "Trim Controls"),
            ControllerInput("STICK_R", InputType.STICK_R, 0.58f, 0.65f, "Orbit Camera"),
            
            ControllerInput("BUTTON_Y", InputType.BUTTON_Y, 0.72f, 0.26f, "Toggle Landing Gear"),
            ControllerInput("BUTTON_X", InputType.BUTTON_X, 0.64f, 0.40f, "Deploy Flaps"),
            ControllerInput("BUTTON_B", InputType.BUTTON_B, 0.80f, 0.40f, "Reverse Thrust"),
            ControllerInput("BUTTON_A", InputType.BUTTON_A, 0.72f, 0.54f, "Wheel Brake"),
            
            ControllerInput("BUTTON_XBOX", InputType.BUTTON_XBOX, 0.50f, 0.25f, "Auto-Pilot Toggle"),
            ControllerInput("BUTTON_VIEW", InputType.BUTTON_VIEW, 0.43f, 0.38f, "Toggle HUD Map"),
            ControllerInput("BUTTON_MENU", InputType.BUTTON_MENU, 0.57f, 0.38f, "Systems Diagnostics"),
            
            ControllerInput("BUMPER_L", InputType.BUMPER_L, 0.26f, 0.14f, "Rudder Left (LB)"),
            ControllerInput("BUMPER_R", InputType.BUMPER_R, 0.74f, 0.14f, "Rudder Right (RB)"),
            ControllerInput("TRIGGER_L", InputType.TRIGGER_L, 0.16f, 0.05f, "Throttle Decel (LT)", 1.2f),
            ControllerInput("TRIGGER_R", InputType.TRIGGER_R, 0.84f, 0.05f, "Throttle Accel (RT)", 1.2f)
        )
    }
}

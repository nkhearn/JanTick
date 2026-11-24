package com.example.data

enum class ChecklistFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    companion object {
        fun fromString(value: String): ChecklistFrequency {
            return entries.firstOrNull { it.name == value } ?: DAILY
        }
    }
}

enum class RunStatus(val label: String) {
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    FINALIZED_INCOMPLETE("Finalized Incomplete");

    companion object {
        fun fromString(value: String): RunStatus {
            return entries.firstOrNull { it.name == value } ?: IN_PROGRESS
        }
    }
}

data class SeedTask(
    val zone: String,
    val title: String
)

object DefaultSchoolTemplates {
    val commonZones = listOf(
        "Entrances & Hallways",
        "Restrooms",
        "Classrooms & Cafeteria",
        "Safety & Mechanical",
        "Grounds & Exterior"
    )

    fun tasksFor(frequency: ChecklistFrequency): List<SeedTask> = when (frequency) {
        ChecklistFrequency.DAILY -> listOf(
            SeedTask("Entrances & Hallways", "Unlock exterior doors and verify panic bars latch smoothly"),
            SeedTask("Entrances & Hallways", "Sweep/mop main foyer, corridors, and clear walkway hazards"),
            SeedTask("Entrances & Hallways", "Sanitize drinking fountains and high-touch door push plates"),
            SeedTask("Restrooms", "Clean and disinfect toilets, urinals, sinks, and mirrors"),
            SeedTask("Restrooms", "Restock soap dispensers, paper towels, and toilet tissue"),
            SeedTask("Restrooms", "Empty restroom waste bins and mop floors with disinfectant"),
            SeedTask("Classrooms & Cafeteria", "Empty classroom and office trash and recycling bins"),
            SeedTask("Classrooms & Cafeteria", "Wipe cafeteria tables, sweep dining floor, and spot-mop spills"),
            SeedTask("Classrooms & Cafeteria", "Check classroom chalkboards/whiteboards, desks, and lighting"),
            SeedTask("Safety & Mechanical", "Confirm fire exit corridors and stairwells are completely unobstructed"),
            SeedTask("Safety & Mechanical", "Walk exterior perimeter for litter, broken glass, or gate issues"),
            SeedTask("Safety & Mechanical", "Secure all perimeter doors, windows, and turn off non-essential lights")
        )
        ChecklistFrequency.WEEKLY -> listOf(
            SeedTask("Entrances & Hallways", "Machine-scrub or deep-mop corridor and stairwell floors"),
            SeedTask("Entrances & Hallways", "Dust locker tops, window sills, baseboards, and display cases"),
            SeedTask("Restrooms", "Descale faucets, flush valves, and pour water into dry floor drains"),
            SeedTask("Restrooms", "Sanitize restroom partition walls, stall latches, and tile grout"),
            SeedTask("Classrooms & Cafeteria", "Vacuum all carpeted classrooms, library, and admin offices"),
            SeedTask("Classrooms & Cafeteria", "Clean interior glass doors, sidelights, and cafeteria serving windows"),
            SeedTask("Safety & Mechanical", "Inspect custodial closets, restock carts, and check chemical labels"),
            SeedTask("Safety & Mechanical", "Check boiler/HVAC room gauges for leaks, odd noises, or alarms"),
            SeedTask("Grounds & Exterior", "Clear storm drains, playground debris, and dumpster enclosure area")
        )
        ChecklistFrequency.MONTHLY -> listOf(
            SeedTask("Safety & Mechanical", "Inspect fire extinguishers (pressure gauge, pin, seal, inspection tag)"),
            SeedTask("Safety & Mechanical", "Test emergency exit signs and backup battery corridor lighting"),
            SeedTask("Safety & Mechanical", "Check HVAC air handling filters and clean return air grilles"),
            SeedTask("Safety & Mechanical", "Flush eyewash stations and science/art room emergency drench showers"),
            SeedTask("Classrooms & Cafeteria", "Wash classroom chair legs, high-dust ceiling vents, and light diffusers"),
            SeedTask("Restrooms", "Deep-scrub restroom tile floors and inspect under-sink plumbing traps"),
            SeedTask("Grounds & Exterior", "Inspect playground equipment bolts, surfacing, and perimeter fencing"),
            SeedTask("Grounds & Exterior", "Check exterior floodlights, roof access hatches, and gutter downspouts")
        )
    }
}

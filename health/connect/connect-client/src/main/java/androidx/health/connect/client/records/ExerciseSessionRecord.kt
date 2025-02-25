/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.health.connect.client.records

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures any exercise a user does. This can be common fitness exercise like running or different
 * sports.
 *
 * Each record needs a start time and end time. Records don't need to be back-to-back or directly
 * after each other, there can be gaps in between.
 *
 * Example code demonstrate how to read exercise session:
 * @sample androidx.health.connect.client.samples.ReadExerciseSessions
 */
public class ExerciseSessionRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    /** Type of exercise (e.g. walking, swimming). Required field. */
    @property:ExerciseTypes public val exerciseType: Int,
    /** Title of the session. Optional field. */
    public val title: String? = null,
    /** Additional notes for the session. Optional field. */
    public val notes: String? = null,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {

    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseSessionRecord) return false

        if (exerciseType != other.exerciseType) return false
        if (title != other.title) return false
        if (notes != other.notes) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + exerciseType.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        /**
         * Metric identifier to retrieve the total exercise time from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val EXERCISE_DURATION_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric(
                dataTypeName = "ActiveTime",
                aggregationType = AggregateMetric.AggregationType.TOTAL,
                fieldName = "time",
            )
        // Exercise duration time requires computing total time from ExerciseEvent/Session and not
        // a straightforward Duration aggregation.

        /**
         * Can be used to represent any generic workout that does not fall into a specific category.
         * Any unknown new value definition will also fall automatically into
         * [EXERCISE_TYPE_OTHER_WORKOUT].
         */
        const val EXERCISE_TYPE_OTHER_WORKOUT = 0
        const val EXERCISE_TYPE_BACK_EXTENSION = 1
        const val EXERCISE_TYPE_BADMINTON = 2
        const val EXERCISE_TYPE_BARBELL_SHOULDER_PRESS = 3
        const val EXERCISE_TYPE_BASEBALL = 4
        const val EXERCISE_TYPE_BASKETBALL = 5
        const val EXERCISE_TYPE_BENCH_PRESS = 6
        const val EXERCISE_TYPE_BENCH_SIT_UP = 7
        const val EXERCISE_TYPE_BIKING = 8
        const val EXERCISE_TYPE_BIKING_STATIONARY = 9
        const val EXERCISE_TYPE_BOOT_CAMP = 10
        const val EXERCISE_TYPE_BOXING = 11
        const val EXERCISE_TYPE_BURPEE = 12
        const val EXERCISE_TYPE_CALISTHENICS = 13
        const val EXERCISE_TYPE_CRICKET = 14
        const val EXERCISE_TYPE_CRUNCH = 15
        const val EXERCISE_TYPE_DANCING = 16
        const val EXERCISE_TYPE_DEADLIFT = 17
        const val EXERCISE_TYPE_DUMBBELL_CURL_LEFT_ARM = 18
        const val EXERCISE_TYPE_DUMBBELL_CURL_RIGHT_ARM = 19
        const val EXERCISE_TYPE_DUMBBELL_FRONT_RAISE = 20
        const val EXERCISE_TYPE_DUMBBELL_LATERAL_RAISE = 21
        const val EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM = 22
        const val EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM = 23
        const val EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM = 24
        const val EXERCISE_TYPE_ELLIPTICAL = 25
        const val EXERCISE_TYPE_EXERCISE_CLASS = 26
        const val EXERCISE_TYPE_FENCING = 27
        const val EXERCISE_TYPE_FOOTBALL_AMERICAN = 28
        const val EXERCISE_TYPE_FOOTBALL_AUSTRALIAN = 29
        const val EXERCISE_TYPE_FORWARD_TWIST = 30
        const val EXERCISE_TYPE_FRISBEE_DISC = 31
        const val EXERCISE_TYPE_GOLF = 32
        const val EXERCISE_TYPE_GUIDED_BREATHING = 33
        const val EXERCISE_TYPE_GYMNASTICS = 34
        const val EXERCISE_TYPE_HANDBALL = 35
        const val EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING = 36
        const val EXERCISE_TYPE_HIKING = 37
        const val EXERCISE_TYPE_ICE_HOCKEY = 38
        const val EXERCISE_TYPE_ICE_SKATING = 39
        const val EXERCISE_TYPE_JUMPING_JACK = 40
        const val EXERCISE_TYPE_JUMP_ROPE = 41
        const val EXERCISE_TYPE_LAT_PULL_DOWN = 42
        const val EXERCISE_TYPE_LUNGE = 43
        const val EXERCISE_TYPE_MARTIAL_ARTS = 44
        const val EXERCISE_TYPE_PADDLING = 46
        const val EXERCISE_TYPE_PARAGLIDING = 47
        const val EXERCISE_TYPE_PILATES = 48
        const val EXERCISE_TYPE_PLANK = 49
        const val EXERCISE_TYPE_RACQUETBALL = 50
        const val EXERCISE_TYPE_ROCK_CLIMBING = 51
        const val EXERCISE_TYPE_ROLLER_HOCKEY = 52
        const val EXERCISE_TYPE_ROWING = 53
        const val EXERCISE_TYPE_ROWING_MACHINE = 54
        const val EXERCISE_TYPE_RUGBY = 55
        const val EXERCISE_TYPE_RUNNING = 56
        const val EXERCISE_TYPE_RUNNING_TREADMILL = 57
        const val EXERCISE_TYPE_SAILING = 58
        const val EXERCISE_TYPE_SCUBA_DIVING = 59
        const val EXERCISE_TYPE_SKATING = 60
        const val EXERCISE_TYPE_SKIING = 61
        const val EXERCISE_TYPE_SNOWBOARDING = 62
        const val EXERCISE_TYPE_SNOWSHOEING = 63
        const val EXERCISE_TYPE_SOCCER = 64
        const val EXERCISE_TYPE_SOFTBALL = 65
        const val EXERCISE_TYPE_SQUASH = 66
        const val EXERCISE_TYPE_SQUAT = 67
        const val EXERCISE_TYPE_STAIR_CLIMBING = 68
        const val EXERCISE_TYPE_STAIR_CLIMBING_MACHINE = 69
        const val EXERCISE_TYPE_STRENGTH_TRAINING = 70
        const val EXERCISE_TYPE_STRETCHING = 71
        const val EXERCISE_TYPE_SURFING = 72
        const val EXERCISE_TYPE_SWIMMING_OPEN_WATER = 73
        const val EXERCISE_TYPE_SWIMMING_POOL = 74
        const val EXERCISE_TYPE_TABLE_TENNIS = 75
        const val EXERCISE_TYPE_TENNIS = 76
        const val EXERCISE_TYPE_UPPER_TWIST = 77
        const val EXERCISE_TYPE_VOLLEYBALL = 78
        const val EXERCISE_TYPE_WALKING = 79
        const val EXERCISE_TYPE_WATER_POLO = 80
        const val EXERCISE_TYPE_WEIGHTLIFTING = 81
        const val EXERCISE_TYPE_WHEELCHAIR = 82
        const val EXERCISE_TYPE_YOGA = 83

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val EXERCISE_TYPE_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                "back_extension" to EXERCISE_TYPE_BACK_EXTENSION,
                "badminton" to EXERCISE_TYPE_BADMINTON,
                "barbell_shoulder_press" to EXERCISE_TYPE_BARBELL_SHOULDER_PRESS,
                "baseball" to EXERCISE_TYPE_BASEBALL,
                "basketball" to EXERCISE_TYPE_BASKETBALL,
                "bench_press" to EXERCISE_TYPE_BENCH_PRESS,
                "bench_sit_up" to EXERCISE_TYPE_BENCH_SIT_UP,
                "biking" to EXERCISE_TYPE_BIKING,
                "biking_stationary" to EXERCISE_TYPE_BIKING_STATIONARY,
                "boot_camp" to EXERCISE_TYPE_BOOT_CAMP,
                "boxing" to EXERCISE_TYPE_BOXING,
                "burpee" to EXERCISE_TYPE_BURPEE,
                "calisthenics" to EXERCISE_TYPE_CALISTHENICS,
                "cricket" to EXERCISE_TYPE_CRICKET,
                "crunch" to EXERCISE_TYPE_CRUNCH,
                "dancing" to EXERCISE_TYPE_DANCING,
                "deadlift" to EXERCISE_TYPE_DEADLIFT,
                "dumbbell_curl_left_arm" to EXERCISE_TYPE_DUMBBELL_CURL_LEFT_ARM,
                "dumbbell_curl_right_arm" to EXERCISE_TYPE_DUMBBELL_CURL_RIGHT_ARM,
                "dumbbell_front_raise" to EXERCISE_TYPE_DUMBBELL_FRONT_RAISE,
                "dumbbell_lateral_raise" to EXERCISE_TYPE_DUMBBELL_LATERAL_RAISE,
                "dumbbell_triceps_extension_left_arm" to
                    EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                "dumbbell_triceps_extension_right_arm" to
                    EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                "dumbbell_triceps_extension_two_arm" to
                    EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                "elliptical" to EXERCISE_TYPE_ELLIPTICAL,
                "exercise_class" to EXERCISE_TYPE_EXERCISE_CLASS,
                "fencing" to EXERCISE_TYPE_FENCING,
                "football_american" to EXERCISE_TYPE_FOOTBALL_AMERICAN,
                "football_australian" to EXERCISE_TYPE_FOOTBALL_AUSTRALIAN,
                "forward_twist" to EXERCISE_TYPE_FORWARD_TWIST,
                "frisbee_disc" to EXERCISE_TYPE_FRISBEE_DISC,
                "golf" to EXERCISE_TYPE_GOLF,
                "guided_breathing" to EXERCISE_TYPE_GUIDED_BREATHING,
                "gymnastics" to EXERCISE_TYPE_GYMNASTICS,
                "handball" to EXERCISE_TYPE_HANDBALL,
                "high_intensity_interval_training" to
                    EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                "hiking" to EXERCISE_TYPE_HIKING,
                "ice_hockey" to EXERCISE_TYPE_ICE_HOCKEY,
                "ice_skating" to EXERCISE_TYPE_ICE_SKATING,
                "jumping_jack" to EXERCISE_TYPE_JUMPING_JACK,
                "jump_rope" to EXERCISE_TYPE_JUMP_ROPE,
                "lat_pull_down" to EXERCISE_TYPE_LAT_PULL_DOWN,
                "lunge" to EXERCISE_TYPE_LUNGE,
                "martial_arts" to EXERCISE_TYPE_MARTIAL_ARTS,
                "paddling" to EXERCISE_TYPE_PADDLING,
                "para_gliding" to
                    EXERCISE_TYPE_PARAGLIDING, // Historic typo in whs with para_gliding
                "pilates" to EXERCISE_TYPE_PILATES,
                "plank" to EXERCISE_TYPE_PLANK,
                "racquetball" to EXERCISE_TYPE_RACQUETBALL,
                "rock_climbing" to EXERCISE_TYPE_ROCK_CLIMBING,
                "roller_hockey" to EXERCISE_TYPE_ROLLER_HOCKEY,
                "rowing" to EXERCISE_TYPE_ROWING,
                "rowing_machine" to EXERCISE_TYPE_ROWING_MACHINE,
                "rugby" to EXERCISE_TYPE_RUGBY,
                "running" to EXERCISE_TYPE_RUNNING,
                "running_treadmill" to EXERCISE_TYPE_RUNNING_TREADMILL,
                "sailing" to EXERCISE_TYPE_SAILING,
                "scuba_diving" to EXERCISE_TYPE_SCUBA_DIVING,
                "skating" to EXERCISE_TYPE_SKATING,
                "skiing" to EXERCISE_TYPE_SKIING,
                "snowboarding" to EXERCISE_TYPE_SNOWBOARDING,
                "snowshoeing" to EXERCISE_TYPE_SNOWSHOEING,
                "soccer" to EXERCISE_TYPE_SOCCER,
                "softball" to EXERCISE_TYPE_SOFTBALL,
                "squash" to EXERCISE_TYPE_SQUASH,
                "squat" to EXERCISE_TYPE_SQUAT,
                "stair_climbing" to EXERCISE_TYPE_STAIR_CLIMBING,
                "stair_climbing_machine" to EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
                "strength_training" to EXERCISE_TYPE_STRENGTH_TRAINING,
                "stretching" to EXERCISE_TYPE_STRETCHING,
                "surfing" to EXERCISE_TYPE_SURFING,
                "swimming_open_water" to EXERCISE_TYPE_SWIMMING_OPEN_WATER,
                "swimming_pool" to EXERCISE_TYPE_SWIMMING_POOL,
                "table_tennis" to EXERCISE_TYPE_TABLE_TENNIS,
                "tennis" to EXERCISE_TYPE_TENNIS,
                "upper_twist" to EXERCISE_TYPE_UPPER_TWIST,
                "volleyball" to EXERCISE_TYPE_VOLLEYBALL,
                "walking" to EXERCISE_TYPE_WALKING,
                "water_polo" to EXERCISE_TYPE_WATER_POLO,
                "weightlifting" to EXERCISE_TYPE_WEIGHTLIFTING,
                "wheelchair" to EXERCISE_TYPE_WHEELCHAIR,
                "workout" to EXERCISE_TYPE_OTHER_WORKOUT,
                "yoga" to EXERCISE_TYPE_YOGA,
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val EXERCISE_TYPE_INT_TO_STRING_MAP =
            EXERCISE_TYPE_STRING_TO_INT_MAP.entries.associateBy({ it.value }, { it.key })
    }

    /**
     * List of supported activities on Health Platform.
     * @suppress
     */
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(
        value =
            [
                EXERCISE_TYPE_BACK_EXTENSION,
                EXERCISE_TYPE_BADMINTON,
                EXERCISE_TYPE_BARBELL_SHOULDER_PRESS,
                EXERCISE_TYPE_BASEBALL,
                EXERCISE_TYPE_BASKETBALL,
                EXERCISE_TYPE_BENCH_PRESS,
                EXERCISE_TYPE_BENCH_SIT_UP,
                EXERCISE_TYPE_BIKING,
                EXERCISE_TYPE_BIKING_STATIONARY,
                EXERCISE_TYPE_BOOT_CAMP,
                EXERCISE_TYPE_BOXING,
                EXERCISE_TYPE_BURPEE,
                EXERCISE_TYPE_CALISTHENICS,
                EXERCISE_TYPE_CRICKET,
                EXERCISE_TYPE_CRUNCH,
                EXERCISE_TYPE_DANCING,
                EXERCISE_TYPE_DEADLIFT,
                EXERCISE_TYPE_DUMBBELL_CURL_LEFT_ARM,
                EXERCISE_TYPE_DUMBBELL_CURL_RIGHT_ARM,
                EXERCISE_TYPE_DUMBBELL_FRONT_RAISE,
                EXERCISE_TYPE_DUMBBELL_LATERAL_RAISE,
                EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                EXERCISE_TYPE_ELLIPTICAL,
                EXERCISE_TYPE_EXERCISE_CLASS,
                EXERCISE_TYPE_FENCING,
                EXERCISE_TYPE_FOOTBALL_AMERICAN,
                EXERCISE_TYPE_FOOTBALL_AUSTRALIAN,
                EXERCISE_TYPE_FORWARD_TWIST,
                EXERCISE_TYPE_FRISBEE_DISC,
                EXERCISE_TYPE_GOLF,
                EXERCISE_TYPE_GUIDED_BREATHING,
                EXERCISE_TYPE_GYMNASTICS,
                EXERCISE_TYPE_HANDBALL,
                EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                EXERCISE_TYPE_HIKING,
                EXERCISE_TYPE_ICE_HOCKEY,
                EXERCISE_TYPE_ICE_SKATING,
                EXERCISE_TYPE_JUMPING_JACK,
                EXERCISE_TYPE_JUMP_ROPE,
                EXERCISE_TYPE_LAT_PULL_DOWN,
                EXERCISE_TYPE_LUNGE,
                EXERCISE_TYPE_MARTIAL_ARTS,
                EXERCISE_TYPE_PADDLING,
                EXERCISE_TYPE_PARAGLIDING,
                EXERCISE_TYPE_PILATES,
                EXERCISE_TYPE_PLANK,
                EXERCISE_TYPE_RACQUETBALL,
                EXERCISE_TYPE_ROCK_CLIMBING,
                EXERCISE_TYPE_ROLLER_HOCKEY,
                EXERCISE_TYPE_ROWING,
                EXERCISE_TYPE_ROWING_MACHINE,
                EXERCISE_TYPE_RUGBY,
                EXERCISE_TYPE_RUNNING,
                EXERCISE_TYPE_RUNNING_TREADMILL,
                EXERCISE_TYPE_SAILING,
                EXERCISE_TYPE_SCUBA_DIVING,
                EXERCISE_TYPE_SKATING,
                EXERCISE_TYPE_SKIING,
                EXERCISE_TYPE_SNOWBOARDING,
                EXERCISE_TYPE_SNOWSHOEING,
                EXERCISE_TYPE_SOCCER,
                EXERCISE_TYPE_SOFTBALL,
                EXERCISE_TYPE_SQUASH,
                EXERCISE_TYPE_SQUAT,
                EXERCISE_TYPE_STAIR_CLIMBING,
                EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
                EXERCISE_TYPE_STRENGTH_TRAINING,
                EXERCISE_TYPE_STRETCHING,
                EXERCISE_TYPE_SURFING,
                EXERCISE_TYPE_SWIMMING_OPEN_WATER,
                EXERCISE_TYPE_SWIMMING_POOL,
                EXERCISE_TYPE_TABLE_TENNIS,
                EXERCISE_TYPE_TENNIS,
                EXERCISE_TYPE_UPPER_TWIST,
                EXERCISE_TYPE_VOLLEYBALL,
                EXERCISE_TYPE_WALKING,
                EXERCISE_TYPE_WATER_POLO,
                EXERCISE_TYPE_WEIGHTLIFTING,
                EXERCISE_TYPE_WHEELCHAIR,
                EXERCISE_TYPE_OTHER_WORKOUT,
                EXERCISE_TYPE_YOGA,
            ]
    )
    annotation class ExerciseTypes
}

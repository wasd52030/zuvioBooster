package pers.wasd52030.zuviobooster.utils

import android.location.Location
import android.util.Log
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import pers.wasd52030.zuviobooster.model.course
import java.util.Date
import java.util.regex.Pattern

class courseUtils {
    companion object {
        fun getCourseList(
            userId: String,
            accessToken: String,
            location: Location
        ): List<course> {

            val res = mutableListOf<course>()
            val url =
                "https://irs.zuvio.com.tw/course/listStudentCurrentCourses?user_id=${userId}&accessToken=${accessToken}"
            Log.d("6", url)

            try {
                val doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .header("Accept", "application/json;charset=UTF-8")
                    .execute()

                val json = JSONObject(doc.body())

                if (json.has("status")) {
                    Log.d("6", "今天是 ${Date()}")
                    Log.d("6", "這學期有修的課為：")

                    val courses = json.getJSONArray("courses")
                    for (i in 0 until courses.length()) {
                        val course = courses.getJSONObject(i)

                        val courseID = course.getString("course_id")
                        val courseName = course.getString("course_name")
                        val teacherName = course.getString("teacher_name")

                        if (!teacherName.contains("Zuvio")) {
                            Log.d("6", "$courseName - $teacherName")
                            res.add(course(courseID, courseName, teacherName))
                        }
                    }

                    for (i in 0 until courses.length()) {
                        val course = courses.getJSONObject(i)

                        val courseID = course.getString("course_id")
                        val courseName = course.getString("course_name")
                        val teacherName = course.getString("teacher_name")

                        if (!teacherName.contains("Zuvio")) {
                            val rollcallId =
                                check(course.getString("course_id"))

                            if (rollcallId != "") {
                                res.map {
                                    if (it.courseID == courseID) {
                                        val c = it
                                        c.checkStatus =
                                            checkIn(userId, accessToken, rollcallId, location)
                                    }
                                    it
                                }
                            }
                        }
                    }
                }

                return res
            } catch (e: Exception) {
                Log.e("6", e.toString())
                throw e
            }
        }

        fun check(courseID: String): String {
            return try {
                val url = "https://irs.zuvio.com.tw/student5/irs/rollcall/$courseID"

                val doc = Jsoup.connect(url).get()
                val pattern = Pattern.compile("var rollcall_id = '(.*?)';")
                val scripts =
                    doc.select("script").filter { it -> pattern.matcher(it.data()).find() }
                val rollcallId = scripts[0].data().split("var rollcall_id = '")[1].split("';")[0]

                rollcallId
            } catch (e: Exception) {
                ""
            }
        }

        fun checkIn(
            userId: String,
            accessToken: String,
            rollcallId: String,
            location: Location
        ): String {
            try {
                val doc = Jsoup.connect("https://irs.zuvio.com.tw/app_v2/makeRollcall")
                    .ignoreContentType(true)
                    .data("user_id", userId)
                    .data("accessToken", accessToken)
                    .data("rollcall_id", rollcallId)
                    .data("device", "WEB")
                    .data("lat", location.latitude.toString())
                    .data("lng", location.longitude.toString())
                    .userAgent("Mozilla")
                    .method(Connection.Method.POST)
                    .execute()

                val json = JSONObject(doc.body())

                if (json.has("status")) {
                    return " - 簽到成功！"
                }
                return " - 簽到失敗：" + json.getString("msg")
            } catch (e: Exception) {
                Log.e("6", e.message.toString())
                throw e
            }
        }
    }
}
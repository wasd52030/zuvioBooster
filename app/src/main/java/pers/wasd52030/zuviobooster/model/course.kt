package pers.wasd52030.zuviobooster.model

data class course(
    val courseID: String,
    val courseName: String,
    val teacherName: String,
    var checkStatus: String = "尚未開放簽到"
)
package com.example.engine

import android.graphics.PointF
import org.json.JSONArray
import org.json.JSONObject

data class CornerPoints(
    var topLeft: PointF = PointF(0.08f, 0.08f),
    var topRight: PointF = PointF(0.92f, 0.08f),
    var bottomRight: PointF = PointF(0.92f, 0.92f),
    var bottomLeft: PointF = PointF(0.08f, 0.92f)
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("tl_x", topLeft.x)
        obj.put("tl_y", topLeft.y)
        obj.put("tr_x", topRight.x)
        obj.put("tr_y", topRight.y)
        obj.put("br_x", bottomRight.x)
        obj.put("br_y", bottomRight.y)
        obj.put("bl_x", bottomLeft.x)
        obj.put("bl_y", bottomLeft.y)
        return obj.toString()
    }

    companion object {
        fun default(): CornerPoints {
            return CornerPoints(
                topLeft = PointF(0.08f, 0.08f),
                topRight = PointF(0.92f, 0.08f),
                bottomRight = PointF(0.92f, 0.92f),
                bottomLeft = PointF(0.08f, 0.92f)
            )
        }

        fun full(): CornerPoints {
            return CornerPoints(
                topLeft = PointF(0f, 0f),
                topRight = PointF(1f, 0f),
                bottomRight = PointF(1f, 1f),
                bottomLeft = PointF(0f, 1f)
            )
        }

        fun fromJson(json: String?): CornerPoints {
            if (json.isNullOrBlank()) return default()
            return try {
                val obj = JSONObject(json)
                CornerPoints(
                    topLeft = PointF(obj.getDouble("tl_x").toFloat(), obj.getDouble("tl_y").toFloat()),
                    topRight = PointF(obj.getDouble("tr_x").toFloat(), obj.getDouble("tr_y").toFloat()),
                    bottomRight = PointF(obj.getDouble("br_x").toFloat(), obj.getDouble("br_y").toFloat()),
                    bottomLeft = PointF(obj.getDouble("bl_x").toFloat(), obj.getDouble("bl_y").toFloat())
                )
            } catch (e: Exception) {
                default()
            }
        }
    }
}

package it.unibo.lss.smart_parking.interface_adapter

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.ktor.http.*
import it.unibo.lss.smart_parking.entity.Center
import it.unibo.lss.smart_parking.entity.ParkingSlot
import it.unibo.lss.smart_parking.use_cases.UseCases
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.bson.Document
import org.bson.conversions.Bson
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.bson.types.ObjectId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/*
Copyright (c) 2022-2023 Antonio Iannotta & Luca Bracchi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
data class InterfaceAdapter(val collection: MongoCollection<Document>): UseCases {
        lateinit var occupyResult: Pair<HttpStatusCode, JsonObject>

        if (this.isSlotOccupied(slotId, parkingSlotList)) {
            occupyResult = createResponse(HttpStatusCode.BadRequest, "errorCode", "ParkingSlotOccupied")
        } else if (!this.isParkingSlotValid(slotId, parkingSlotList)) {
            occupyResult = createResponse(HttpStatusCode.NotFound, "errorCode", "ParkingSlotNotValid")
        } else {
            val filter = Filters.eq("id", slotId)
            val updates = emptyList<Bson>().toMutableList()
            updates.add(Updates.set("occupied", true))
            updates.add(Updates.set("stopEnd", stopEnd))
            updates.add(Updates.set("userId", userId))

            val options = UpdateOptions().upsert(true)
            collection.updateOne(filter, updates, options)

            occupyResult = createResponse(HttpStatusCode.OK, "successCode", "Success")
        }

        return occupyResult
    }

    override fun incrementOccupation(userId: String, slotId: String, stopEnd: Instant): Pair<HttpStatusCode, JsonObject> {
        val parkingSlot = getParkingSlot(slotId)

        val incrementResult: Pair<HttpStatusCode, JsonObject>

        if (parkingSlot == null) {
            incrementResult = createResponse(HttpStatusCode.NotFound, "errorCode", "ParkingSlotNotFound")
        } else if (!parkingSlot.occupied || parkingSlot.stopEnd == null) {
            incrementResult = createResponse(HttpStatusCode.BadRequest, "errorCode", "ParkingSlotFree")
        } else if (!this.isTimeValid(stopEnd, parkingSlot.stopEnd)) {
            incrementResult = createResponse(HttpStatusCode.BadRequest, "errorCode", "InvalidParkingSlotStopEnd")
        } else {
            val filters = mutableListOf<Bson>()
            filters.add(Filters.eq("_id", ObjectId(slotId)))
            filters.add(Filters.eq("occupierId", ObjectId(userId)))
            val filter = Filters.and(filters)
            val update = Updates.set("stopEnd", stopEnd.toJavaInstant())

            collection.updateOne(filter, update)

            incrementResult = createResponse(HttpStatusCode.OK, "successCode", "Success")
        }
        return incrementResult
    }

    override fun freeSlot(slotId: String): Pair<HttpStatusCode, JsonObject> {
        val parkingSlot = getParkingSlot(slotId)

        val freeResult: Pair<HttpStatusCode, JsonObject>

        if (parkingSlot == null) {
            freeResult = createResponse(HttpStatusCode.NotFound, "errorCode", "ParkingSlotNotFound")
        } else if (!parkingSlot.occupied) {
            // Nothing to do, parking slot is already free
            freeResult = createResponse(HttpStatusCode.OK, "successCode", "Success")
        } else {
            val filter = Filters.eq("_id", ObjectId(slotId))
            val updates = Updates.combine(
                Updates.set("occupied", false),
                Updates.unset("stopEnd"),
                Updates.unset("occupierId")
            )

            collection.updateOne(filter, updates)

            freeResult = createResponse(HttpStatusCode.OK, "successCode", "Success")
        }
        return freeResult
    }

    override fun getAllParkingSlotsByRadius(center: Center): List<ParkingSlot> {
        val parkingSlotList = emptyList<ParkingSlot>().toMutableList()

        collection
            .find(Filters.geoWithinCenterSphere("location", center.position.longitude, center.position.latitude, center.radius / 6371))
            .forEach {
                    document ->  parkingSlotList.add(createParkingSlotFromDocument(document))
            }
        return parkingSlotList
    }

    override fun getParkingSlotList(): List<ParkingSlot> {
        val parkingSlotList = emptyList<ParkingSlot>().toMutableList()

        collection.find().forEach {
                document -> parkingSlotList.add(createParkingSlotFromDocument(document))
        }
        return parkingSlotList
    }

    override fun getParkingSlot(id: String): ParkingSlot? {
        val filter = Filters.eq("_id", ObjectId(id))
        val dbParkingSlot = collection.find(filter).first()

        val parkingSlot: ParkingSlot? = if (dbParkingSlot != null) {
            createParkingSlotFromDocument(dbParkingSlot)
        } else {
            null
        }
        return parkingSlot
    }

    override fun getParkingSlotOccupiedByUser(userId: String): ParkingSlot? {
        val filter = Filters.eq("occupierId", ObjectId(userId))

        val parkingSlotDocument = collection.find(filter).first()

        val parkingSlot: ParkingSlot? = if (parkingSlotDocument != null) {
            createParkingSlotFromDocument(parkingSlotDocument)
        } else {
            null
        }
        return parkingSlot
    }

    fun createResponse(httpStatusCode: HttpStatusCode, code: String, message: String): Pair<HttpStatusCode, JsonObject> {
        val jsonElement = mutableMapOf<String, JsonElement>()
        jsonElement[code] = Json.parseToJsonElement(message)
        val jsonObject = JsonObject(jsonElement)

        return Pair(httpStatusCode, jsonObject)
    }

    private fun createParkingSlotFromDocument(document: Document): ParkingSlot {
        print(((document["location"] as Document)["coordinates"]))
        return ParkingSlot(
            document["id"].toString(),
            document["occupied"].toString().toBoolean(),
            document["stopEnd"].toString(),
            returnCoordinates((document["location"] as Document)["coordinates"].toString())[1].toDouble(),
            returnCoordinates((document["location"] as Document)["coordinates"].toString())[0].toDouble(),
            document["userId"].toString()
        )
    }

    private fun returnCoordinates(coordinates: String): List<String> {
        var coordinatesDropped = coordinates.drop(1)
        coordinatesDropped = coordinatesDropped.dropLast(1)
        return coordinatesDropped.split(",")
    }

}

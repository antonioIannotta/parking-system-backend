package it.unibo.lss.parking_system.use_cases

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import it.unibo.lss.parking_system.entity.UserInfo
import java.util.*

fun getUserInfo(email: String): UserInfo? {

    val mongoClient = getMongoClient()
    val mongoCollection = getUserCollection(mongoClient)

    val filter = Filters.eq("email", email)
    val project = Projections.exclude("password")
    val userInfoDocument = mongoCollection.find(filter).projection(project).first()

    mongoClient.close()

    return if (Objects.isNull(userInfoDocument)) null
    else UserInfo(
        userInfoDocument?.get("email").toString(),
        userInfoDocument?.get("name").toString(),
    )

}
package com.example.user.com.example.use_cases.user

import com.example.entity.user.UserInfo
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import java.util.*

fun getUserInfo(mail: String): UserInfo? {

    val mongoClient = getMongoClient()
    val mongoCollection = getUserCollection(mongoClient)

    val filter = Filters.eq("email", mail)
    val project = Projections.exclude("password")
    val userInfoDocument = mongoCollection.find(filter).projection(project).first()

    mongoClient.close()

    return if (Objects.isNull(userInfoDocument)) null
    else UserInfo(
        userInfoDocument?.get("email").toString(),
        userInfoDocument?.get("name").toString(),
        userInfoDocument?.get("surname").toString(),
    )

}
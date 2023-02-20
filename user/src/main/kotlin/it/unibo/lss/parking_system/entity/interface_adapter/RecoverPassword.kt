package it.unibo.lss.parking_system.entity.interface_adapter

import it.unibo.lss.parking_system.entity.interface_adapter.model.ResponseCode
import it.unibo.lss.parking_system.entity.interface_adapter.model.response.ServerResponseBody
import it.unibo.lss.parking_system.entity.interface_adapter.utils.generateJWT
import it.unibo.lss.parking_system.entity.interface_adapter.utils.getRecoverPasswordMailContent
import it.unibo.lss.parking_system.entity.interface_adapter.utils.sendMail
import it.unibo.lss.parking_system.use_cases.getUserInfo
import java.util.*

fun recoverPassword(email: String, tokenSecret: String): ServerResponseBody {

    return if (!Objects.isNull(getUserInfo(email))) {
        val jwt = generateJWT(email, tokenSecret)
        sendMail(
            email,
            "Password recovery mail",
            getRecoverPasswordMailContent(jwt)
        )
        ServerResponseBody(ResponseCode.SUCCESS.code, "success")
    } else ServerResponseBody(ResponseCode.USER_NOT_FOUND.code, "User not found")

}
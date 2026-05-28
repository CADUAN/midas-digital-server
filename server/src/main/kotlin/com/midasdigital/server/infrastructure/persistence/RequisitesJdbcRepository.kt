package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.error.NotFoundException
import com.midasdigital.server.domain.model.PaymentRequisitesRecord
import com.midasdigital.server.domain.repository.RequisitesRepository

class RequisitesJdbcRepository : RequisitesRepository {

    override fun getPaymentRequisites(userId: Long): PaymentRequisitesRecord {
        return DatabaseFactory.withConnection { connection ->
            connection.runInTransaction {
                findPaymentRequisites(connection, userId)?.let { return@runInTransaction it }

                createPaymentRequisites(connection, userId)

                findPaymentRequisites(connection, userId)
                    ?: throw NotFoundException("Реквизиты не найдены")
            }
        }
    }
}

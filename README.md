package ru.banksoyuz.dbofl.processor.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import ru.banksoyuz.dbofl.abs.client.dto.AbsSbpC2cTransferResponse
import ru.banksoyuz.dbofl.abs.client.dto.enum.AbsDocumentStatus
import ru.banksoyuz.dbofl.entity.DocumentEntity
import ru.banksoyuz.dbofl.entity.enums.DocumentStatus
import ru.banksoyuz.dbofl.exception.EntityNotFoundException
import ru.banksoyuz.dbofl.processor.AbsC2cTransferResponseProcessor
import ru.banksoyuz.dbofl.processor.DocumentProcessor
import ru.banksoyuz.dbofl.repository.DocumentRepository
import java.time.LocalDateTime

@Component
class AbsC2cTransferResponseProcessorImpl(
    private val documentRepository: DocumentRepository,
    private val documentProcessors: List<DocumentProcessor>
) : AbsC2cTransferResponseProcessor {

    override suspend fun process(absC2cDocumentDataResponse: AbsSbpC2cTransferResponse) = coroutineScope {
        //todo исправить тип document id на long
        val documentId = absC2cDocumentDataResponse.sbpC2cDocumentId!!.toLong()
        val documentEntity = async { documentRepository.findById(documentId) }.await()
            ?: throw EntityNotFoundException("Document not found for $documentId")

        updateDocument(absC2cDocumentDataResponse, documentEntity)
        async { documentRepository.save(documentEntity) }.await()
        documentProcessors.forEach { it.process(documentEntity) }
    }

    private fun updateDocument(
        absC2cDocumentDataResponse: AbsSbpC2cTransferResponse,
        documentEntity: DocumentEntity,
    ) {
        //todo испрвить в апи status на non null
        when (absC2cDocumentDataResponse.status!!) {
            AbsDocumentStatus.ACCEPTED -> {
                if (documentEntity.isSelfBankTransfer == true) {
                    documentEntity.documentstatus = DocumentStatus.END
                    documentEntity.executeStamp = LocalDateTime.now()
                } else {
                    documentEntity.documentstatus = DocumentStatus.FOR_SEND_PC
                }
            }

            AbsDocumentStatus.REJECTED, AbsDocumentStatus.TIME_OUT -> {
                documentEntity.documentstatus = DocumentStatus.FOR_DECLINE
                documentEntity.declineStamp = LocalDateTime.now()
                //todo publs
                // ish decline message
            }

            AbsDocumentStatus.PENDING -> {
                documentEntity.documentstatus = DocumentStatus.SEND_ABS
            }
        }
        documentEntity.absStatus = absC2cDocumentDataResponse.status
        documentEntity.extRef = absC2cDocumentDataResponse.absDocumentId
        absC2cDocumentDataResponse.errText.let { documentEntity.declineInfo = absC2cDocumentDataResponse.errText }
        absC2cDocumentDataResponse.errCode.let { documentEntity.declineInfo = absC2cDocumentDataResponse.errCode }
    }
}

package com.hulkhiretech.payments.dao.interfaces;

import com.hulkhiretech.payments.dto.TransactionDTO;

public interface TransactionDao {
	
	public TransactionDTO createTransaction(TransactionDTO txnDto);
	
	public TransactionDTO getTransactionByReference(String txnReference);
	
	public TransactionDTO updateTransactionStatusDetails(TransactionDTO dto);
	
	public TransactionDTO getTransactionByProviderReference(String providerReference);
	
}

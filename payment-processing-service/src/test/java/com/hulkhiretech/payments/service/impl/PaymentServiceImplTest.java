package com.hulkhiretech.payments.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.gson.Gson;
import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.InitiatePaymentDTO;
import com.hulkhiretech.payments.dto.PaymentResDTO;
import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.exception.ProcessingException;
import com.hulkhiretech.payments.http.HttpServiceEngine;
import com.hulkhiretech.payments.service.interfaces.PaymentStatusService;
import com.hulkhiretech.payments.stripeprovider.CreatePaymentReq;
import com.hulkhiretech.payments.stripeprovider.PaymentRes;
import com.hulkhiretech.payments.util.GsonUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * The Class PaymentServiceImplTest.
 * We write unittestcases for testing PaymentServiceImpl class.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

	@Mock
	private PaymentStatusService paymentStatusService;

	@Mock
	private TransactionDao transactionDao;

	@Mock
	private ModelMapper modelMapper;

	@Mock
	private Gson gson;

	@Mock
	private HttpServiceEngine httpServiceEngine;

	@Mock
	private GsonUtils gsonUtils;

	@InjectMocks
	private PaymentServiceImpl paymentServiceImpl;

	/**
	 * When calling createPayment, we are passing TransactionDTO object as argument.
	 * we expect that for this TransactionDTO, the status should be set to CREATED 
	 * & txnReference should be generated.
	 * 
	 * If this happens, means our logic is working as expected.
	 */
	@Test
	public void testCreatePayment() {
		log.info("Test case for createPayment method|paymentServiceImpl:{}"
				, paymentServiceImpl);

		//Arrange
		TransactionDTO txnDto = new TransactionDTO();

		//Act
		paymentServiceImpl.createPayment(txnDto);

		//Assert
		assertEquals(TransactionStatusEnum.CREATED.getName(), 
				txnDto.getTxnStatus());
		assertNotNull(txnDto.getTxnReference());

	}

	/**
	 * we pass txnRef. Expectation is, if in DB we dont find txnRef, 
	 * then transactionDto should be null.
	 * For null TransactionDTO, functional code throws exception.
	 */
	@Test
	public void testInitiatePaymentNoTxnExist() {
		log.info("Test case for createPayment method|paymentServiceImpl:{}"
				, paymentServiceImpl);

		//Arrange
		String txnRef = "txnRef-ref1";
		InitiatePaymentDTO reqDto = new InitiatePaymentDTO();

		//Act
		ProcessingException processingExp = assertThrows(
				ProcessingException.class, () -> 
				paymentServiceImpl.initiatePayment(txnRef, reqDto));

		//Assert
		assertNotNull(processingExp);
		assertEquals(HttpStatus.BAD_REQUEST, processingExp.getHttpStatus());

		assertEquals(ErrorCodeEnum.INVALID_TXN_REFERENCE.getErrorCode(), 
				processingExp.getErrorCode());

		assertEquals(ErrorCodeEnum.INVALID_TXN_REFERENCE.getErrorMessage(), 
				processingExp.getErrorMessage());

	}

	@Test
	public void testInitiatePaymentSuccess() {
		log.info("Test case for createPayment method|paymentServiceImpl:{}"
				, paymentServiceImpl);

		//Arrange
		String txnRef = "txnRef-ref1";
		InitiatePaymentDTO reqDto = new InitiatePaymentDTO();

		TransactionDTO txnDto = new TransactionDTO();
		when(transactionDao.getTransactionByReference(txnRef)).thenReturn(txnDto);

		ResponseEntity<String> httpResponse = new ResponseEntity<String>("", 
				HttpStatus.CREATED);
		when(httpServiceEngine.makeHttpCall(any())).thenReturn(httpResponse);

		PaymentRes paymentRes = new PaymentRes();
		paymentRes.setUrl("http://test.com");
		when(gsonUtils.fromJson(anyString(), eq(PaymentRes.class))
				).thenReturn(paymentRes);


		when(modelMapper.map(any(), eq(CreatePaymentReq.class))
				).thenReturn(new CreatePaymentReq());


		PaymentResDTO paymentResDTO = new PaymentResDTO();
		paymentResDTO.setId("provider-ref-id1");
		paymentResDTO.setUrl("http://test.com");
		
		when(modelMapper.map(any(), eq(PaymentResDTO.class)))
		.thenReturn(paymentResDTO);

		//Act
		TransactionDTO txnDtoResponse = paymentServiceImpl.initiatePayment(txnRef, reqDto);

		//Assert
		assertNotNull(txnDtoResponse);
		assertEquals(TransactionStatusEnum.PENDING.getName(), 
				txnDtoResponse.getTxnStatus());
		assertEquals("provider-ref-id1", 
				txnDtoResponse.getProviderReference());
		assertEquals("http://test.com", 
				txnDtoResponse.getUrl());

	}

}
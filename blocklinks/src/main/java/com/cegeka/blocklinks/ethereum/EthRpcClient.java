package com.cegeka.blocklinks.ethereum;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;

import com.cegeka.blocklinks.ethereum.crypto.CryptoUtil;
import com.cegeka.blocklinks.ethereum.pojo.Block;
import com.cegeka.blocklinks.ethereum.pojo.CompileOutput;
import com.cegeka.blocklinks.ethereum.pojo.Transaction;
import com.cegeka.blocklinks.ethereum.pojo.TransactionCall;
import com.cegeka.blocklinks.ethereum.pojo.TransactionReceipt;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

/**
 * Class for rpc request invoker to ethereum client.
 * 
 * @author Andrei Grigoriu
 *
 */
public class EthRpcClient {

	/**
	 * Ethereum rpc Interface
	 */
	private EthRpcInterface rpc;
	public final static String defaultHostname = "localhost";
	public final static int defaultPort = 8545;

	public EthRpcClient() {
		this(defaultHostname, defaultPort);
	}

	/**
	 * 
	 * @param hostname
	 *            for ethereum client
	 * @param port
	 *            for ethereum client
	 */
	public EthRpcClient(String hostname, int port) {
		URL url;
		try {
			url = new URL("http://" + hostname + ":" + port + "/");
			JsonRpcHttpClient rpcClient = new JsonRpcHttpClient(url);
			rpc = ProxyUtil.createClientProxy(getClass().getClassLoader(), EthRpcInterface.class, rpcClient);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return coinbase from ethereum client
	 * @throws JsonRpcClientException
	 */
	public String getCoinbase() throws JsonRpcClientException {
		return rpc.eth_coinbase();
	}

	/**
	 * 
	 * @return accounts from ethereum client
	 * @throws JsonRpcClientException
	 */
	public String[] getAccounts() throws JsonRpcClientException {
		return rpc.eth_accounts();
	}

	/**
	 * 
	 * @param address
	 *            to get nonce of
	 * @return nonce of address
	 * @throws JsonRpcClientException
	 */
	public BigInteger getAccountNonce(String address) throws JsonRpcClientException {
		String txCount = rpc.eth_getTransactionCount(address, "latest");
		return CryptoUtil.hexToBigInteger(txCount);
	}

	/**
	 * EXPERIMENTAL (should not be used), only works with custom ethereum
	 * clients
	 * 
	 * @param address
	 *            to unlock
	 * @param secret
	 *            (passphrase)
	 * @return true if unlocked
	 * @throws JsonRpcClientException
	 */
	public boolean unlockAccount(String address, String secret) throws JsonRpcClientException {
		return rpc.personal_unlockAccount(address, secret);
	}

	/**
	 * EXPERIMENTAL (should not be used), only works with custom ethereum
	 * clients
	 * 
	 * @param from
	 *            address to send from
	 * @param fromSecret
	 *            passphrase from
	 * @param to
	 *            address to send to
	 * @param valueWei
	 *            total wei to send
	 * @return transaction hash
	 * @throws JsonRpcClientException
	 */
	public String sendTransaction(String from, String fromSecret, String to, BigInteger valueWei)
			throws JsonRpcClientException {
		boolean unlock = rpc.personal_unlockAccount(from, fromSecret);

		if (unlock) {
			return this.sendTransaction(from, to, valueWei);
		}
		return null;
	}

	/**
	 * Send transaction from already unlocked accounts.
	 * 
	 * @param from
	 *            address to send from
	 * @param to
	 *            address to send to
	 * @param valueWei
	 *            wei to send
	 * @return transaction hash
	 * @throws JsonRpcClientException
	 */
	public String sendTransaction(String from, String to, BigInteger valueWei) throws JsonRpcClientException {
		Transaction t = new Transaction();

		t.setFrom(from.toString());
		t.setTo(to);
		t.setValue(valueWei.toString());

		return rpc.eth_sendTransaction(t);
	}

	/**
	 * Send self encoded transaction. The safest way to rpc send transactions.
	 * 
	 * @param encodedSignedTransaction
	 *            encoded data as hex
	 * @return transaction hash
	 * @throws JsonRpcClientException
	 */
	public String sendRawTransaction(String encodedSignedTransaction) throws JsonRpcClientException {
		return rpc.eth_sendRawTransaction(encodedSignedTransaction);
	}

	/**
	 * Send self encoded transaction. The safest way to rpc send transactions.
	 * 
	 * @param encodedSignedTransaction
	 *            encoded data
	 * @return transaction hash
	 * @throws JsonRpcClientException
	 */
	public String sendRawTransaction(byte[] encodedSignedTransaction) throws JsonRpcClientException {
		return sendRawTransaction(CryptoUtil.byteToHex(encodedSignedTransaction));
	}

	/**
	 * Get balance of address
	 * 
	 * @param address
	 *            to get balance of
	 * @return balance as wei
	 * @throws JsonRpcClientException
	 */
	public BigInteger getBalance(String address) throws JsonRpcClientException {
		String balance = rpc.eth_getBalance(address);
		return CryptoUtil.hexToBigInteger(balance);
	}

	/**
	 * Returns the transaction receipt, null if the transaction is not mined.
	 * 
	 * @param txHash
	 *            to get receipt of
	 * @return receipt
	 * @throws JsonRpcClientException
	 */
	public TransactionReceipt getTransactionReceipt(String txHash) throws JsonRpcClientException {
		return rpc.eth_getTransactionReceipt(txHash);
	}

	/**
	 * Get transaction data by transaction hash/
	 * 
	 * @param txHash
	 *            to get data by.
	 * @return transaction data.
	 * @throws JsonRpcClientException
	 */
	public Transaction getTransaction(String txHash) throws JsonRpcClientException {
		return rpc.eth_getTransactionByHash(txHash);
	}

	/**
	 * Call a contract method or dry call it.
	 * 
	 * @param call
	 *            to make
	 * @return output encoded
	 */
	public String callMethod(TransactionCall call) {
		return rpc.eth_call(call);
	}

	/**
	 * Call a contract method or dry call it.
	 * 
	 * @param call
	 *            to make
	 * @return output encoded
	 */
	public String callMethod(EthCall call) {
		return rpc.eth_call(call.getCall());
	}

	/**
	 * Get latest block gas limit
	 * 
	 * @return gas limit of latest block on ethereum client
	 */
	public BigInteger getLatestBlockGasLimit() {
		Block block = rpc.eth_getBlockByNumber("latest", true);
		if (block != null) {
			return CryptoUtil.hexToBigInteger(block.gasLimit);
		}

		return null;
	}

	/**
	 * Compile solidity source on ethereum client and return a compile output.
	 * 
	 * @param sourceCode
	 * @return
	 */
	public CompileOutput compileSolidity(String sourceCode) {
		return rpc.eth_compileSolidity(sourceCode);
	}
}
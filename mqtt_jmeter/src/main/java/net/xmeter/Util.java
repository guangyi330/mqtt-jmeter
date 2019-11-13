package net.xmeter;

import net.xmeter.samplers.AbstractMQTTSampler;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.jmeter.services.FileServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.logging.Logger;

public class Util implements Constants {
	
	private static SecureRandom random = new SecureRandom();
    private static char[] seeds = "abcdefghijklmnopqrstuvwxmy0123456789".toCharArray();
    private static final Logger logger = Logger.getLogger(Util.class.getCanonicalName());

	public static String generateClientId(String prefix) {
		int leng = prefix.length();
		int postLeng = MAX_CLIENT_ID_LENGTH - leng;
		if (postLeng < 0) {
			throw new IllegalArgumentException("ClientId prefix " + prefix + " is too long, max allowed is "
					+ MAX_CLIENT_ID_LENGTH + " but was " + leng);
		}
		UUID uuid = UUID.randomUUID();
		String string = uuid.toString().replace("-", "");
		String post = string.substring(0, postLeng);
		return prefix + post;
	}

	public static SSLContext getContext(AbstractMQTTSampler sampler) throws Exception {
		if (!sampler.isDualSSLAuth()) {
			SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} }, new SecureRandom());
			return sslContext;
		} else {
			logger.info("Configured with dual SSL, trying to load key files.");
			String KEYSTORE_PASS = sampler.getKeyStorePassword();
			String CLIENTCERT_PASS = sampler.getClientCertPassword();

			File theFile1 = getKeyStoreFile(sampler);
			File theFile2 = getClientCertFile(sampler);
			
			try(InputStream is_cacert = new FileInputStream(theFile1); InputStream is_client = new FileInputStream(theFile2)) {
				KeyStore tks = KeyStore.getInstance(KeyStore.getDefaultType()); // jks
				tks.load(is_cacert, KEYSTORE_PASS.toCharArray());

				KeyStore cks = KeyStore.getInstance("PKCS12");
				cks.load(is_client, CLIENTCERT_PASS.toCharArray());

				SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(tks, new TrustSelfSignedStrategy()) // use it to customize
						.loadKeyMaterial(cks, CLIENTCERT_PASS.toCharArray()) // load client certificate
						.build();
				return sslContext;
			}
		}
	}

	public static File getKeyStoreFile(AbstractMQTTSampler sampler) {
		return getFilePath(sampler.getKeyStoreFilePath());
	}

	public static File getClientCertFile(AbstractMQTTSampler sampler) {
		return getFilePath(sampler.getClientCertFilePath());
	}

	private static File getFilePath(String filePath) {
		String baseDir = FileServer.getFileServer().getBaseDir();
		if(baseDir != null && (!baseDir.endsWith("/"))) {
			baseDir += "/";
		}

		File theFile = new File(filePath);
		if(!theFile.exists()) {
			filePath = baseDir + filePath;
			theFile = new File(filePath);
			if(!theFile.exists()) {
				throw new RuntimeException("Cannot find file : " + filePath);
			}
		}
		return theFile;
	}

	public static String generatePayload(int size) {
		StringBuffer res = new StringBuffer();
		for(int i = 0; i < size; i++) {
			res.append(seeds[random.nextInt(seeds.length - 1)]);
		}
		return res.toString();
	}

	public static boolean isSecureProtocol(String protocol) {
		return SSL_PROTOCOL.equals(protocol) || WSS_PROTOCOL.equals(protocol);
	}

	public static boolean isWebSocketProtocol(String protocol) {
		return WS_PROTOCOL.equals(protocol) || WSS_PROTOCOL.equals(protocol);
	}
}

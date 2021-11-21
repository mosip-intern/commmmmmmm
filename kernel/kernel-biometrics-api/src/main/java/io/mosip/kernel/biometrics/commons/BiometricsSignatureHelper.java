package io.mosip.kernel.biometrics.commons;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.Entry;
import io.mosip.kernel.core.exception.BiometricSignatureValidationException;

public class BiometricsSignatureHelper {

	public static String extractJWTToken(BIR bir) throws BiometricSignatureValidationException, JSONException {

		String constructedJWTToken = null;

		List<Entry> othersInfo = bir.getOthers();
		if (othersInfo == null) {
			throw new BiometricSignatureValidationException("Others value is null inside BIR");
		}

		String sb = new String(bir.getSb(), StandardCharsets.UTF_8);
		String bdb = Base64.getUrlEncoder().encodeToString(bir.getBdb());

		for (Entry entry : othersInfo) {
			if (entry.getKey().equals("PAYLOAD")) {
				String value = entry.getValue().replace("<bioValue>", bdb);
				String encodedPayloadValue = Base64.getUrlEncoder().encodeToString(value.getBytes());
				constructedJWTToken = constructJWTToken(sb, encodedPayloadValue);
				JSONObject jsonObject = new JSONObject(value);
				String digitalID = jsonObject.getString("digitalId");
				compareJWTForSameCertificates(constructedJWTToken, digitalID);
			}
		}
		return constructedJWTToken;

	}

	private static void compareJWTForSameCertificates(String jwtString1, String jwtString2)
			throws JSONException, BiometricSignatureValidationException {
		String jwtString1Header = new String( Base64.getUrlDecoder().decode(jwtString1.split("\\.")[0]));
		JSONObject jwtString1HeaderCertificate = new JSONObject(jwtString1Header);
		JSONArray jwtString1HeadercertificateJsonArray = jwtString1HeaderCertificate.getJSONArray("x5c");
		ArrayList<String> jwtString1Certificates = new ArrayList<String>();
		if (jwtString1HeadercertificateJsonArray != null) {
			for (int i = 0; i < jwtString1HeadercertificateJsonArray.length(); i++) {
				jwtString1Certificates.add(jwtString1HeadercertificateJsonArray.getString(i));
			}
		}

		String jwtString2Header = new String(Base64.getUrlDecoder().decode(jwtString2.split("\\.")[0]));
		JSONObject jwtString2HeaderCertificate = new JSONObject(jwtString2Header);
		JSONArray jwtString2HeadercertificateJsonArray = jwtString2HeaderCertificate.getJSONArray("x5c");
		ArrayList<String> jwtString2Certificates = new ArrayList<String>();
		if (jwtString2HeadercertificateJsonArray != null) {
			for (int i = 0; i < jwtString2HeadercertificateJsonArray.length(); i++) {
				jwtString2Certificates.add(jwtString2HeadercertificateJsonArray.getString(i));
			}
		}

		if (!jwtString1Certificates.containsAll(jwtString2Certificates)) {
			throw new BiometricSignatureValidationException("Header Certificate mismatch");
		}
	}

	private static String constructJWTToken(String sb, String encodedPayloadValue) {
		return sb.replace("..", "." + encodedPayloadValue + ".");
	}
}
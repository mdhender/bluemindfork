package net.bluemind.user.persistance.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import net.bluemind.core.api.fault.ServerFault;

public final class MD5Hash implements Hash {
	private static final String HEX_DIGITS = "0123456789abcdef";
	private static final Pattern pattern = Pattern.compile("[0-9a-z]{32}");

	@Override
	public String create(String plaintext) throws ServerFault {
		MessageDigest mg = null;
		try {
			mg = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new ServerFault(e);
		}
		mg.update(plaintext.getBytes());
		return toHexString(mg.digest());
	}

	@Override
	public boolean validate(String plaintext, String hash) throws ServerFault {
		return create(plaintext).equals(hash);
	}

	private String toHexString(byte[] param) {
		StringBuffer sb = new StringBuffer(param.length * 2);
		for (int i = 0; i < param.length; i++) {
			int b = param[i] & 0xFF;
			sb.append(HEX_DIGITS.charAt(b >>> 4)).append(HEX_DIGITS.charAt(b & 0xF));
		}
		return sb.toString();
	}

	@Override
	public boolean matchesAlgorithm(String password) {
		return pattern.matcher(password).matches();
	}

}

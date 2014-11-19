package com.lizard.fastdb.util;

import java.net.InetAddress;
import java.util.StringTokenizer;

/**
 * IP工具类，用于IP的转换
 * 
 * @author SHEN.GANG
 */
public class IPUtils 
{

	/**
	 * long IP 转String IP
	 * 
	 * @param ip
	 *            long IP 地址
	 * @return 字符串 IP 地址
	 */
	public static String convertIPToString(long ip) {
		String rtn = "";
		try {
			byte[] by = new byte[4];

			InetAddress address = InetAddress.getByAddress(long2byte(by, ip, 0));
			rtn = address.getHostAddress();
			// 倒序
			StringBuffer sb = new StringBuffer();
			String ids[] = parseToken2String(rtn, ".");
			// 应该是192.168.0.223
			sb.append(ids[3]);
			sb.append(".");
			sb.append(ids[2]);
			sb.append(".");
			sb.append(ids[1]);
			sb.append(".");
			sb.append(ids[0]);
			rtn = sb.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
		return rtn;
	}

	/**
	 * 字符串IP转long IP
	 * 
	 * @param ipStr 字符串ip地址
	 * @return 返回long的ip地址
	 */
	public static long convertIPToLong(String ipStr) {
		try {
			if (ipStr == null || ipStr.length() == 0)
				return 0;
			long addr = 0;
			String b = "";
			int tmpFlag1 = 0, tmpFlag2 = 0;
			tmpFlag1 = 0;
			tmpFlag2 = ipStr.indexOf(".");
			b = ipStr.substring(tmpFlag1, tmpFlag2);
			addr = new Long(b).longValue();
			tmpFlag1 = tmpFlag2 + 1;
			tmpFlag2 = ipStr.indexOf(".", tmpFlag1);
			addr = addr << 8;
			b = ipStr.substring(tmpFlag1, tmpFlag2);
			addr += new Long(b).longValue();
			tmpFlag1 = tmpFlag2 + 1;
			tmpFlag2 = ipStr.indexOf(".", tmpFlag1);
			addr = addr << 8;
			b = ipStr.substring(tmpFlag1, tmpFlag2);
			addr += new Long(b).longValue();
			addr = addr << 8;
			addr += new Long(ipStr.substring(tmpFlag2 + 1, ipStr.length())).longValue();
			if (addr > 2147483647) {
				addr -= Long.parseLong("4294967296");
			}
			return addr;
		} catch (Exception e) {
			return 0;
		}
	}
	
	private static String[] parseToken2String(String str, String delim) {
	  String strRtn[] = null;
	  StringTokenizer st = new StringTokenizer(str, delim);
	  int length = st.countTokens();
	  strRtn = new String[length];
	  int i = 0;
	  while (st.hasMoreTokens()) {
	    String str1 = st.nextToken().trim();
	    strRtn[i] = str1;
	    i++;
	  }
	  return strRtn;
	}
	
	private static byte[] long2byte(byte[] out, long in, int offset) {
	  if (out.length > 0) {
	    out[offset] = (byte) in;
	  }
	  if (out.length > 1) {
	    out[offset + 1] = (byte) (in >>> 8);
	  }
	  if (out.length > 2) {
	    out[offset + 2] = (byte) (in >>> 16);
	  }
	  if (out.length > 3) {
	    out[offset + 3] = (byte) (in >>> 24);
	  }
	  if (out.length > 4) {
	    out[offset + 4] = (byte) (in >>> 32);
	  }
	  if (out.length > 5) {
	    out[offset + 5] = (byte) (in >>> 40);
	  }
	  if (out.length > 6) {
	    out[offset + 6] = (byte) (in >>> 48);
	  }
	  if (out.length > 7) {
	    out[offset + 7] = (byte) (in >>> 56);
	  }
	  return out;
	}
	
	
	
}

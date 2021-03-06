/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package avanzadagroup.net.google;

import avanzadagroup.net.altanAPI.responses.AddressCoordinatesResp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sapienter.jbilling.common.FormatLogger;

/**
 *
 * @author Arturo Ruiz
 */
public class AddressCoordinates {
	private static final FormatLogger LOG = new FormatLogger(
			Logger.getLogger(AddressCoordinates.class));

	public AddressCoordinatesResp getCoordinates(String streetName,
			String streetNumber, String zipCode, String city, String state,
			String Country) {
		int intentos = 0;

		AddressCoordinatesResp acr = new AddressCoordinatesResp();
		acr.setStatus("error");

		do {

			URL url;
			HttpURLConnection connection = null;
			try {
				String sURL = "https://maps.google.com/maps/api/geocode/json?"
						+ "address=" + URLEncoder.encode(streetName + " " + streetNumber + ", "
						+ city + "," + state + "" + ", " + zipCode + ","
						+ Country
						//+ "&key=AIzaSyAz7lp-M3oF9Pw7SiJ_h30FGT4TQiO2pVo"
						, "UTF-8");

				url = new URL(sURL.replace(" ", "%20"));
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");

				connection.setRequestProperty("Content-Type",
						"application/json; charset=UTF-8");
				connection
						.setRequestProperty("Accept",
								"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				connection.setRequestProperty("Accept-Language",
						"es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3");
				// connection.setRequestProperty("Accept-Encoding",
				// "gzip, deflate");

				connection.setRequestProperty("Cache-Control", "max-age=0");
				connection
						.setRequestProperty("User-Agent",
								"Mozilla/5.0 (Windows NT 6.1; rv:35.0) Gecko/20100101 Firefox/35.0");
				connection.setRequestProperty("Pragma", "no-cache");

				connection.setDoInput(true);
				connection.setDoOutput(true);

				BufferedReader br;

				if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
					br = new BufferedReader(new InputStreamReader(
							connection.getInputStream()));
				} else {
					br = new BufferedReader(new InputStreamReader(
							connection.getErrorStream()));
				}
				
				

				String inputLine;

				StringBuilder buf = new StringBuilder();
				while ((inputLine = br.readLine()) != null) {
					buf.append(inputLine);

				}

				LOG.debug("CBOSS:: Google API" + buf.toString());

				br.close();
				connection.disconnect();

				org.json.JSONObject jsonObj = new JSONObject(buf.toString());

				org.json.JSONArray results = jsonObj.getJSONArray("results");

				acr.setStatus(connection.getResponseCode() + "");
				acr.setStatusDescription("Cordenadas Obtenidas");
				acr.setLatitude(results.getJSONObject(0)
						.getJSONObject("geometry").getJSONObject("location")
						.getDouble("lat")
						+ "");
				acr.setLongitude(results.getJSONObject(0)
						.getJSONObject("geometry").getJSONObject("location")
						.getDouble("lng")
						+ "");

			} catch (Exception e) {
				LOG.debug("CBOSS::" + e);
				acr.setStatus("error");
				acr.setStatusDescription("Error al obtener coordenadas");

			}
			intentos++;
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while (acr.getStatus().equals("error") && intentos < 10);
		return acr;

	}

}

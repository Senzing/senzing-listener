package com.senzing.listener.senzing.service.g2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.senzing.g2.engine.G2Engine;
import com.senzing.g2.engine.G2JNI;
import com.senzing.listener.senzing.service.exception.ServiceExecutionException;
import com.senzing.listener.senzing.service.exception.ServiceSetupException;

/**
 * This class handles communication with G2.  It sets up an instance of G2 and interacts with it (get entities etc.).
 */
public class G2Service {
  protected G2Engine g2Engine;
  static final String moduleName = "G2JNI";

  /**
   * Default constructor.
   */
  public G2Service() {
  }

  /**
   * Initializes the service. It reads the information from the ini file and
   * sets up G2 using that data.
   * 
   * @param iniFile
   * 
   * @throws ServiceSetupException
   */

  public void init(String iniFile) throws ServiceSetupException {
    boolean verboseLogging = false;

    String configData = null;
    try {
      configData = getG2IniDataAsJson(iniFile);
    } catch (IOException | JSONException e) {
      throw new ServiceSetupException(e);
    }
    g2Engine = new G2JNI();
    int result = g2Engine.initV2(moduleName, configData, verboseLogging);
    if (result != G2ServiceDefinitions.G2_VALID_RESULT) {
      StringBuilder errorMessage = new StringBuilder("G2 engine failed to initalize with error: ");
      errorMessage.append(g2ErrorMessage(g2Engine));
      throw new ServiceSetupException(errorMessage.toString());
    }
  }

  /**
   * Gets an entity for an entity id.
   * 
   * @param g2EntiyId The G2 id of the entity
   * @param includeFullFeatures If true full features are returned. Could have performance impact
   * @param includeFeatureStats If true, statistics for features are returned. Could have performance impact
   * 
   * @return Entity information in JSON format
   * 
   * @throws ServiceExecutionException
   */
  public String getEntity(long g2EntiyId, boolean includeFullFeatures, boolean includeFeatureStats) throws ServiceExecutionException {
    StringBuffer response = new StringBuffer();
    int flags;
    if (!(includeFullFeatures || includeFeatureStats)) {
      flags = G2Engine.G2_ENTITY_DEFAULT_FLAGS;
    } else {
      flags = G2Engine.G2_ENTITY_INCLUDE_ALL_RELATIONS;
      if (includeFullFeatures) {
        flags |= G2Engine.G2_ENTITY_INCLUDE_ALL_FEATURES;
      }
      if (includeFeatureStats) {
        flags |= G2Engine.G2_ENTITY_SHOW_FEATURES_STATS;
      }
    }
    int result = g2Engine.getEntityByEntityIDV2(g2EntiyId, flags, response);
    if (result != G2ServiceDefinitions.G2_VALID_RESULT) {
      StringBuilder errorMessage = new StringBuilder("G2 engine failed to retrieve an entity with error: ");
      errorMessage.append(g2ErrorMessage(g2Engine));
      throw new ServiceExecutionException(errorMessage.toString());
    }
    return response.toString();

  }

  /**
   * Gets and entity for a data source and record id.
   * 
   * @param dataSource
   * @param recordId
   * @return
   * @throws ServiceExecutionException
   */
  public String getEntity(String dataSource, String recordId) throws ServiceExecutionException {
    StringBuffer response = new StringBuffer();
    int result = g2Engine.getEntityByRecordIDV2(dataSource, recordId, G2Engine.G2_ENTITY_DEFAULT_FLAGS, response);
    if (result != G2ServiceDefinitions.G2_VALID_RESULT) {
      StringBuilder errorMessage = new StringBuilder("G2 engine failed to retrieve an entity with error: ");
      errorMessage.append(g2ErrorMessage(g2Engine));
      throw new ServiceExecutionException(errorMessage.toString());
    }
    return response.toString();
  }

  public String searchByAttribute(String criteria) throws ServiceExecutionException {
    StringBuffer response = new StringBuffer();
    int result = g2Engine.searchByAttributesV2(criteria, G2Engine.G2_ENTITY_DEFAULT_FLAGS, response);
    if (result != G2ServiceDefinitions.G2_VALID_RESULT) {
      StringBuilder errorMessage = new StringBuilder("G2 engine failed to retrieve an entity with error: ");
      errorMessage.append(g2ErrorMessage(g2Engine));
      throw new ServiceExecutionException(errorMessage.toString());
    }
    return response.toString();
  }

  public String exportConfig() throws ServiceExecutionException {
    StringBuffer response = new StringBuffer();
    int result = g2Engine.exportConfig(response);
    if (result != G2ServiceDefinitions.G2_VALID_RESULT) {
      StringBuilder errorMessage = new StringBuilder("G2 engine failed to export configuration with error: ");
      errorMessage.append(g2ErrorMessage(g2Engine));
      throw new ServiceExecutionException(errorMessage.toString());
    }
    return response.toString();
  }

  static protected String g2ErrorMessage(G2Engine g2Engine) {
    return g2Engine.getLastExceptionCode() + ", " + g2Engine.getLastException();
  }

  private static String getG2IniDataAsJson(String iniFile) throws IOException, JSONException {
    Pattern  iniSection  = Pattern.compile( "\\s*\\[([^]]*)\\]\\s*" );
    Pattern  iniKeyValue = Pattern.compile( "\\s*([^=]*)=(.*)" );
    JSONObject rootObject = new JSONObject();
    try (Scanner scanner = new Scanner(new File(iniFile))) {
      JSONObject currentSection = null;
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (line.startsWith("#")) {
          continue;
        }
        Matcher matcher = iniSection.matcher(line);
        if (matcher.matches()) {
          currentSection = new JSONObject();
          rootObject.put(matcher.group(1), currentSection);
        } else if (currentSection != null) {
          matcher = iniKeyValue.matcher(line);
          if (matcher.matches()) {
            currentSection.put(matcher.group(1), matcher.group(2));
          }
        }
      }
    }
    return rootObject.toString();
  }
}
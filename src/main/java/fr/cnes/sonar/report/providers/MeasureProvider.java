/*
 * This file is part of cnesreport.
 *
 * cnesreport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * cnesreport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with cnesreport.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnes.sonar.report.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import fr.cnes.sonar.report.exceptions.BadSonarQubeRequestException;
import fr.cnes.sonar.report.exceptions.SonarQubeException;
import fr.cnes.sonar.report.model.Measure;
import fr.cnes.sonar.report.model.SonarQubeServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Provides issue items
 */
public class MeasureProvider extends AbstractDataProvider {

    /**
     * Parameter "projectStatus" of the JSON response
     */
    private static final String PROJECT_STATUS = "projectStatus";
    /**
     * Parameter "conditions" of the JSON response
     */
    private static final String CONDITIONS = "conditions";
    /**
     * Parameter "status" of the JSON response
     */
    private static final String STATUS = "status";
    /**
     * Parameter "metricKey" of the JSON response
     */
    private static final String METRIC_KEY = "metricKey";
    /**
     * Parameter "metrics" of the JSON response
     */
    private static final String METRICS = "metrics";
    /**
     * Parameter "name" of the JSON response
     */
    private static final String NAME = "name";


    /**
     * Complete constructor
     * @param pServer SonarQube server..
     * @param pToken String representing the user token.
     * @param pProject The id of the project to report.
     * @param pBranch The branch of the project to report.
     */
    public MeasureProvider(final SonarQubeServer pServer, final String pToken, final String pProject,
            final String pBranch) {
        super(pServer, pToken, pProject, pBranch);
    }

    /**
     * Get all the measures of a project
     * @return Array containing all the measures
     * @throws BadSonarQubeRequestException when the server does not understand the request
     * @throws SonarQubeException When SonarQube server is not callable.
     */
    public List<Measure> getMeasures() throws BadSonarQubeRequestException, SonarQubeException {
        // send a request to sonarqube server and return th response as a json object
        // if there is an error on server side this method throws an exception
        final JsonObject jo = request(String.format(getRequest(GET_MEASURES_REQUEST),
                getServer().getUrl(), getProjectKey(), getBranch()));

        // json element containing measure information
        final JsonElement measuresJE = jo.get(COMPONENT).getAsJsonObject().get(MEASURES);
        // put json in a list of measures
        final Measure[] tmp = (getGson().fromJson(measuresJE, Measure[].class));

        // then add all measure to the results list
        // return the list
        return new ArrayList<>(Arrays.asList(tmp));
    }

    /**
     * Get the quality gate status of a project
     * @return Map containing each condition of the quality gate and its status
     * @throws BadSonarQubeRequestException when the server does not understand the request
     * @throws SonarQubeException When SonarQube server is not callable.
     */
    public Map<String, String> getQualityGateStatus() throws BadSonarQubeRequestException, SonarQubeException {
        // request to get the quality gate status
        final JsonObject projectStatusResult = request(String.format(getRequest(GET_QUALITY_GATE_STATUS_REQUEST),
                getServer().getUrl(), getBranch(), getProjectKey()));
        // map containing the result
        Map<String, String> res = new LinkedHashMap<>();
        // retrieve the content of the object
        JsonObject projectStatusObject = projectStatusResult.get(PROJECT_STATUS).getAsJsonObject();
        // retrieve the array of conditions
        JsonArray conditions = projectStatusObject.get(CONDITIONS).getAsJsonArray();
        // add a couple metric name / status to the map for each condition
        for (JsonElement condition : conditions) {
            JsonObject conditionObject = condition.getAsJsonObject();
            String status = conditionObject.get(STATUS).getAsString();
            String metricKey = conditionObject.get(METRIC_KEY).getAsString();
            final JsonObject metricResult = request(String.format(getRequest(GET_METRIC_REQUEST),
                getServer().getUrl(), getBranch(), getProjectKey(), metricKey));
            String name = metricResult.get(METRICS).getAsJsonArray().get(0).getAsJsonObject().get(NAME).getAsString();
            res.put(name, status);
        }
        return res;
    }
}

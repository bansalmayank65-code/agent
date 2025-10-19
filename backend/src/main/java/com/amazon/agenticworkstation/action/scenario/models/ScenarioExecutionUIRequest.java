package com.amazon.agenticworkstation.action.scenario.models;

import java.util.List;

import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService.ScenarioExecutionRequest;

public class ScenarioExecutionUIRequest {
	private List<ScenarioExecutionRequest> scenarioExecutionRequests;

	public List<ScenarioExecutionRequest> getRequests() {
		return scenarioExecutionRequests;
	}

	public void setRequests(List<ScenarioExecutionRequest> requests) {
		this.scenarioExecutionRequests = requests;
	}

	@Override
	public String toString() {
		return "ScenarioExecutionUIRequest [scenarioExecutionRequests=" + scenarioExecutionRequests + "]";
	}

}

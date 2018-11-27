package com.rabidgremlin.concord.config;

import java.io.UnsupportedEncodingException;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class ConcordServerConfiguration
    extends Configuration
{  
  @NotEmpty
  private String jwtTokenSecret;

  @Min(value=1)
  private int consensusLevel;

  @JsonProperty("jwtTokenSecret")
  public void setJwtTokenSecret(String secret)
  {
	jwtTokenSecret = secret;
  }

  public byte[] getJwtTokenSecret() throws UnsupportedEncodingException {
	return jwtTokenSecret.getBytes("UTF-8");
  }

	@JsonProperty("consensusLevel")
	public void setConsensusLevel(int level)
	{
		consensusLevel = level;
	}

	public int getConsensusLevel() {
		return consensusLevel;
	}

	@Valid
	@NotNull
	private DataSourceFactory database = new DataSourceFactory();

	
	public DataSourceFactory getDatabase() {
		return database;
	}
	
	

	public void getDataSourceFactory(DataSourceFactory database) {
		this.database = database;
	}
	
	@Valid
	@NotNull
	private PluginConfig labelSuggester;

	public PluginConfig getLabelSuggester() {
		return labelSuggester;
	}
	
	
	@Valid
	@NotNull
	private PluginConfig credentialsValidator;

	public PluginConfig getCredentialsValidator() {
		return credentialsValidator;
	}

	
	
}

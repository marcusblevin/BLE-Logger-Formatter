package graphet;

import java.util.Date;

public class Reading {
	private Long x = null;
	private Double y = null;
	private Integer series = null;
	
	public Reading(Long time, Double degC, Integer s) {
		x = time;
		y = degC;
		series = s;
	}
	
	public Long getTimeLong() {
		return x;
	}
	public Date getTimeUTC() {
		return new Date(x);
	}
	public Double getDegC() {
		return y;
	}
	public Integer getSeries() {
		return series;
	}
}
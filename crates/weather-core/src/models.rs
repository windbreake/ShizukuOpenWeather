use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WeatherSummary {
    pub location_name: String,
    pub latitude: f64,
    pub longitude: f64,
    pub temperature: f64,
    pub description: String,
    pub source: String,
    pub cached: bool,
}

(ns clj-mcp-example.core
  (:require [co.gaiwan.mcp :as mcp]
            [co.gaiwan.mcp.state :as state]
            [clojure.data.json :as json]
            [taoensso.telemere :as tel]
            [clojure.string :as str]
            [clj-http.client :as client]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HTTP Requests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def NWS_API_BASE "https://api.weather.gov")
(def USER_AGENT "weather-app/1.0")

(defn get-nws
  [url]
  (client/get url
              {:headers {"User-Agent" USER_AGENT
                         "Accept" "application/geo+json"}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Processing API responses
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn feature->alert
  [feature]
  (let [properties (get feature "properties")]
    (str "Event: " (get properties "event" "Unknown") "\n"
         "Area: " (get properties "areaDesc" "Unknown")  "\n"
         "Severity: " (get properties "severity" "Unknown")  "\n"
         "Description: " (get properties "description" "Unknown")  "\n"
         "Instructions: " (get properties "instruction" "Unknown")  "\n")))

(defn get-alerts
  [state-str]
  (tel/log! {:level :info,
             :msg "get alerts"})
  (let [url (str NWS_API_BASE "/alerts/active/area/" state-str)
        resp (get-nws url)
        features (-> (:body resp)
                     (json/read-str)
                     (get "features"))]
    (map feature->alert features)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MCP
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(state/add-tool
 {:name "weather_alert"
  :title "Weather Alert Tool"
  :description "Given a two letter state code, finds the weather alerts for that state."
  :schema {"type" "object"
           "properties" {"state" {"type" "string"
                                  "description" "Two letter state code"}}
           "required" ["state"]}
  :tool-fn (fn [_req {:keys [state]}]
             (let [alerts (get-alerts state)]
               (tel/log! {:level :info, :msg alerts})
               {:content {:type :text
                          :text (str/join "\n" (vec alerts))}
                :isError true}))})

;; Start MCP
(mcp/run-http! {:port 3999})

(clojure.string/join "\n" (get-alerts "VA"))


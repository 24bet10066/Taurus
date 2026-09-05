import os
import json
from collections import defaultdict
from datetime import datetime

def calculate_ema(series, alpha=0.3):
    """Calculate Exponential Moving Average as baseline in pure Python."""
    if not series:
        return []
    ema = [series[0]]
    for i in range(1, len(series)):
        ema.append(alpha * series[i] + (1 - alpha) * ema[-1])
    return ema

def mean_absolute_error(y_true, y_pred):
    if not y_true: return 0
    return sum(abs(t - p) for t, p in zip(y_true, y_pred)) / len(y_true)

class SimpleLinearRegression:
    def __init__(self):
        self.weights = []
        
    def fit(self, X, y):
        # Extremely simplified gradient descent for linear regression in pure Python
        n_features = len(X[0])
        self.weights = [0.0] * n_features
        bias = 0.0
        learning_rate = 0.001
        epochs = 1000
        
        for _ in range(epochs):
            for i in range(len(X)):
                prediction = sum(w * x for w, x in zip(self.weights, X[i])) + bias
                error = prediction - y[i]
                
                for j in range(n_features):
                    self.weights[j] -= learning_rate * error * X[i][j]
                bias -= learning_rate * error

    def predict(self, X):
        preds = []
        for row in X:
            preds.append(sum(w * x for w, x in zip(self.weights, row)))
        return preds

def main():
    print("Starting MLOps Forecasting Pipeline for Parts Demand...")
    
    import psycopg2
    try:
        conn = psycopg2.connect("postgresql://postgres:postgres@localhost:5432/serviceos_jobs")
        cur = conn.cursor()
        query = """
        SELECT DATE(added_at) as date, SUM(quantity) as demand 
        FROM job_parts_used 
        GROUP BY DATE(added_at)
        ORDER BY date ASC
        """
        cur.execute(query)
        rows = cur.fetchall()
        conn.close()
        
        if len(rows) < 30:
            print("Not enough data in DB to train model (need at least 30 days). Exiting.")
            return
            
    except Exception as e:
        print(f"Error connecting to database: {e}")
        return

    # Prepare data
    demand_series = [float(row[1]) for row in rows]
    dates = [row[0] for row in rows]
    
    # Feature Engineering for ML Model
    X = []
    y = []
    for i in range(7, len(demand_series)):
        day_of_week = dates[i].weekday()
        lag_1 = demand_series[i-1]
        lag_7 = demand_series[i-7]
        X.append([day_of_week, lag_1, lag_7])
        y.append(demand_series[i])
        
    # Train-test split (last 20 days for testing)
    split_idx = len(X) - 20
    X_train, X_test = X[:split_idx], X[split_idx:]
    y_train, y_test = y[:split_idx], y[split_idx:]
    
    # 3. Baseline: EMA
    # Predict the next day using EMA of previous days
    ema_full = calculate_ema(demand_series[:-1])
    ema_test_preds = ema_full[-20:]
    baseline_mae = mean_absolute_error(y_test, ema_test_preds)
    
    # 4. Train ML Model
    model = SimpleLinearRegression()
    model.fit(X_train, y_train)
    ml_preds = model.predict(X_test)
    ml_mae = mean_absolute_error(y_test, ml_preds)
    
    print("-" * 40)
    print("EVALUATION RESULTS")
    print(f"Baseline (EMA) MAE: {baseline_mae:.2f}")
    print(f"ML Model (LinReg) MAE:  {ml_mae:.2f}")
    print("-" * 40)
    
    # 5. Programmatic Decision (Reject if ML model is not significantly better)
    improvement_threshold = 0.10 # ML model must be 10% better to justify complexity
    target_mae = baseline_mae * (1 - improvement_threshold)
    
    if ml_mae <= target_mae:
        print(f"SUCCESS: ML model provides significant improvement (>{improvement_threshold*100}% lower error).")
        model_path = "model.json"
        with open(model_path, "w") as f:
            json.dump({"weights": model.weights}, f)
        print(f"Model saved to {model_path} and ready for deployment.")
    else:
        print("REJECTED: ML model does not provide enough improvement over the EMA baseline.")
        print("Action: Sticking to simple EMA forecasting to avoid unnecessary operational complexity.")
        if os.path.exists("model.json"):
            os.remove("model.json")

if __name__ == "__main__":
    main()

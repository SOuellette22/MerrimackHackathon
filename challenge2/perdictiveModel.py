import streamlit as st
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

from sklearn.model_selection import train_test_split
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LinearRegression
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import OneHotEncoder
from sklearn.metrics import r2_score, mean_absolute_error, mean_squared_error

st.title("EPA GHG Emissions Prediction Portal 🌎 — Linear Regression")
st.markdown("""
Trains a **Linear Regression (OLS)** model to predict `total_ghg_emissions_tonnes`
from **state**, **industry_sector**, and **reporting_year**.  
Evaluates R², MAE, RMSE, and accuracy %, and lets you make custom predictions.  
Now also shows **which states and industries drive emissions the most** using the **trained predictor**.
""")

df = pd.read_csv("epa_ghgrp_2021_2023_aggregate.csv")
st.subheader("Dataset Preview")
st.dataframe(df.head())

# ---- Columns per spec ----
target_col = "total_ghg_emissions_tonnes"
required_features = ["state", "industry_sector", "reporting_year"]

if not all(c in df.columns for c in [target_col] + required_features):
    st.error(f"Dataset must contain: {', '.join([target_col] + required_features)}")
else:
    X = df[required_features].copy()
    y = df[target_col].copy()

    # ---- Train/test split (80/20) ----
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    # ---- Preprocessing ----
    cat_features = ["state", "industry_sector"]
    num_features = ["reporting_year"]

    cat_tfm = Pipeline([
        ("imputer", SimpleImputer(strategy="most_frequent")),
        ("onehot", OneHotEncoder(handle_unknown="ignore")),
    ])
    num_tfm = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
    ])

    preprocessor = ColumnTransformer([
        ("cat", cat_tfm, cat_features),
        ("num", num_tfm, num_features),
    ])

    # ---- Linear Regression model ----
    model = Pipeline([
        ("preprocess", preprocessor),
        ("linreg", LinearRegression()),
    ])

    # ---- Fit ----
    model.fit(X_train, y_train)

    # ---- Evaluate ----
    y_pred = model.predict(X_test)
    r2 = r2_score(y_test, y_pred)
    mae = mean_absolute_error(y_test, y_pred)
    rmse = mean_squared_error(y_test, y_pred)
    mean_y = float(np.mean(y_test)) if len(y_test) else np.nan
    acc_pct = (max(0.0, 1.0 - rmse / mean_y) * 100.0) if (mean_y and mean_y != 0) else np.nan

    st.subheader("Model Performance")
    c1, c2, c3, c4 = st.columns(4)
    c1.metric("R²", f"{r2:.4f}")
    c2.metric("MAE", f"{mae:,.2f}")
    c4.metric("Accuracy (≈ 1 − RMSE/mean(y))", f"{acc_pct:.2f}%")

    # Average predicted emissions per state and per industry
    # ---- NEW: Model-based drivers (states & industries) ----
    st.subheader("Model-Estimated Emission Drivers")
    st.caption("Rankings use **predicted** emissions from the trained model (average per group).")

    # Use the trained pipeline to predict for all rows, then aggregate
    df_pred = df.copy()
    df_pred["predicted_emissions"] = model.predict(df_pred[required_features])

    # Average predicted emissions per state and per industry
    state_preds = (
        df_pred.groupby("state")["predicted_emissions"]
        .mean()
        .sort_values(ascending=False)
        .head(10)
    )
    industry_preds = (
        df_pred.groupby("industry_sector")["predicted_emissions"]
        .mean()
        .sort_values(ascending=False)
        .head(10)
    )

    cA, cB = st.columns(2)
    with cA:
        st.markdown("#### 🔝 States Driving Emissions (Model-Based)")
        fig1, ax1 = plt.subplots(figsize=(7, 4))
        ax1.barh(state_preds.index[::-1], state_preds.values[::-1])
        ax1.set_xlabel("Predicted Avg Emissions (tonnes CO₂e)")
        ax1.set_ylabel("State")
        ax1.set_title("Top 10 States by Predicted GHG Emissions")
        st.pyplot(fig1)
        st.write(f"**Top State:** {state_preds.index[0]} — {state_preds.iloc[0]:,.0f} tonnes (avg predicted)")

    with cB:
        st.markdown("#### 🏭 Industries Driving Emissions (Model-Based)")
        fig2, ax2 = plt.subplots(figsize=(7, 4))
        ax2.barh(industry_preds.index[::-1], industry_preds.values[::-1])
        ax2.set_xlabel("Predicted Avg Emissions (tonnes CO₂e)")
        ax2.set_ylabel("Industry Sector")
        ax2.set_title("Top 10 Industries by Predicted GHG Emissions")
        st.pyplot(fig2)
        st.write(f"**Top Industry:** {industry_preds.index[0]} — {industry_preds.iloc[0]:,.0f} tonnes (avg predicted)")

    # ---- Custom Prediction Form ----
    st.subheader("Predict Emissions for a Custom Input")
    with st.form("predict_form"):
        state_input = st.selectbox(
            "State", sorted(pd.Series(df["state"].dropna().unique()).astype(str))
        )
        sector_input = st.selectbox(
            "Industry Sector", sorted(pd.Series(df["industry_sector"].dropna().unique()).astype(str))
        )
        year_input = st.selectbox(
            "Reporting Year", sorted(pd.Series(df["reporting_year"].dropna().unique()).astype(int))
        )
        submitted = st.form_submit_button("Predict Emissions")

    if submitted:
        user_df = pd.DataFrame({
            "state": [state_input],
            "industry_sector": [sector_input],
            "reporting_year": [int(year_input)]
        })
        pred = float(model.predict(user_df)[0])
        st.success(f"Estimated Total GHG Emissions: **{pred:,.2f} tonnes CO₂e**")


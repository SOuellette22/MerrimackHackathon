import streamlit as st
import pandas as pd
import plotly.graph_objs as go

st.set_page_config(page_title="Main Page", page_icon="🏠", layout="wide")

df = pd.read_csv('epa_ghgrp_2021_2023_aggregate.csv')

st.title("EPA Greenhouse Gas Data Dashboard")

def display_top_10_sec(mean_amount, sectors):
    data = {"Mean Amount": mean_amount}
    df_top = pd.DataFrame(data, index=sectors)

    df_sorted = df_top.sort_values(by="Mean Amount", ascending=False)
    top_sectors = df_sorted.index.to_numpy()
    top_amount = df_sorted["Mean Amount"].to_numpy()

    color_scale = [[0, 'rgb(204, 153, 0)'], [1, 'rgb(255, 215, 0)']]

    fig = go.Figure()
    fig.add_trace(go.Bar(
        x=top_sectors[:10],
        y=top_amount[:10],
        name='Top Sectors',
        marker=dict(
            color=top_amount[:10],
            colorscale=color_scale,
            showscale=True
        )
    ))

    fig.update_layout(
        title='Top 10 Emitting Sectors',
        xaxis_title='Sector',
        yaxis_title='Amount of Greenhouse Gasses',
        template='plotly_white'
    )

    st.plotly_chart(fig)

# Compute mean per industry sector
sector_means = df.groupby('industry_sector')['total_ghg_emissions_tonnes'].mean()

# Call the function
display_top_10_sec(sector_means.values, sector_means.index)

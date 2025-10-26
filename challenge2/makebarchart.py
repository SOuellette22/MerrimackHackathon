import streamlit as st
import pandas as pd
import plotly.graph_objs as go


df = pd.read_csv('epa_ghgrp_2021_2023_aggregate.csv')
st.set_page_config(
    page_title="Main Page",
    page_icon="🏠",
    layout="wide",
)

def display_top_10_sec(mean_amount, sectors):
    # Example DataFrame where the sectors are the index and returns are the values
    data = {
        "Mean Amount": mean_amount
    }

    # Create DataFrame
    df = pd.DataFrame(data, index=sectors)

    # Sort the DataFrame by 'Mean Amount' column in descending order
    df_sorted = df.sort_values(by="Mean Amount", ascending=False)

    # Extract top sectors and their associated returns as arrays
    top_sectors = df_sorted.index.to_numpy()  # Extract sectors
    top_amount = df_sorted["Mean Amount"].to_numpy()  # Extract returns


    color_scale = [
        [0, 'rgb(204, 153, 0)'],  # Darker gold
        [1, 'rgb(255, 215, 0)']   # Bright gold
    ]

    # Plot the bar chart with the custom color scale
    fig = go.Figure()
    fig.add_trace(go.Bar(
        x=top_sectors[:10],  # Show top 10 sectors
        y=top_amount[:10],
        name='Top Sectors',
        marker=dict(
            color=top_amount[:10],  # Apply color based on the returns
            colorscale=color_scale,  # Use the custom color scale
            showscale=True           # Show the color scale bar
        )
    ))

    fig.update_layout(
        title='Top 10 Emitting Sectors',
        xaxis_title='Sector',
        yaxis_title='Amount of Greenhouse Gasses',
        template='plotly_white'
    )

    st.plotly_chart(fig)

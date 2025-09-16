package com.example.gostreetball.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.gostreetball.ui.SortType

@Composable
fun SortSegmentedControl(
    selectedSort: SortType,
    onSortSelected: (SortType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    ) {
        SortSegmentButton(
            text = "None",
            selected = selectedSort == SortType.NONE,
            onClick = { onSortSelected(SortType.NONE) },
            modifier = Modifier.weight(1f)
        )
        SortSegmentButton(
            text = "Name",
            selected = selectedSort == SortType.NAME_ASC || selectedSort == SortType.NAME_DESC,
            ascending = selectedSort == SortType.NAME_ASC,
            onClick = {
                val next = if (selectedSort == SortType.NAME_ASC) SortType.NAME_DESC else SortType.NAME_ASC
                onSortSelected(next)
            },
            modifier = Modifier.weight(1f)
        )
        SortSegmentButton(
            text = "Rating",
            selected = selectedSort == SortType.RATING_ASC || selectedSort == SortType.RATING_DESC,
            ascending = selectedSort == SortType.RATING_ASC,
            onClick = {
                val next = if (selectedSort == SortType.RATING_ASC) SortType.RATING_DESC else SortType.RATING_ASC
                onSortSelected(next)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SortSegmentButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    ascending: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                shape = RectangleShape
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            if (selected && text != "None") {
                Icon(
                    imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Sort Direction",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }
    }
}

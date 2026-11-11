package com.example.podcast.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.podcast.data.model.EpisodeSortOrder

@Composable
fun SortChip(
    currentSort: EpisodeSortOrder,
    onSortSelected: (EpisodeSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(currentSort.displayName) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort"
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            EpisodeSortOrder.values().forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.displayName) },
                    onClick = {
                        onSortSelected(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

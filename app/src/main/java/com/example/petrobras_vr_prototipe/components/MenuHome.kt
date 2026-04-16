package com.example.petrobras_vr_prototipe.components

import androidx.compose.foundation.BorderStroke
import com.example.petrobras_vr_prototipe.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.petrobras_vr_prototipe.screens.VrAppState

@Composable
fun MenuHome(onNavigate: (VrAppState) -> Unit) {
    Row(
        modifier = Modifier.graphicsLayer(
            rotationY = 15f,
            rotationX = -10f,
            cameraDistance = 12f
        )
            .padding(horizontal = 16.dp)
            .width(315.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(15.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween

    ) {
        Image(
            painter = painterResource(R.drawable.icone_petrobras),
            contentDescription = "Icone Petrobras",
            modifier = Modifier.size(40.dp)
        )
        Row(modifier = Modifier.clickable { onNavigate(VrAppState.TASKS) }, horizontalArrangement = Arrangement.Center , verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.icone_tarefas),
                contentDescription = "Icone Tarefas",
                modifier = Modifier.size(40.dp)
            )
            Icon(
                painter = painterResource(id = R.drawable.icone_down_arrow),
                contentDescription = "Descrição do ícone",
                modifier = Modifier.size(10.dp),
                tint = Color.Unspecified
            )
        }
        Row(modifier = Modifier.clickable { onNavigate(VrAppState.NETWORKING) },horizontalArrangement = Arrangement.Center , verticalAlignment = Alignment.CenterVertically)  {
            Image(
                painter = painterResource(R.drawable.icone_networking),
                contentDescription = "Icone Networking",
                modifier = Modifier.size(40.dp)
            )
            Icon(
                painter = painterResource(id = R.drawable.icone_down_arrow),
                contentDescription = "Descrição do ícone",
                modifier = Modifier.size(10.dp),
                tint = Color.Unspecified
            )
        }
        Row(horizontalArrangement = Arrangement.Center , verticalAlignment = Alignment.CenterVertically)  {
            Image(
                painter = painterResource(R.drawable.icone_settings),
                contentDescription = "Icone Configurações",
                modifier = Modifier.size(40.dp)
            )
            Icon(
                painter = painterResource(id = R.drawable.icone_down_arrow),
                contentDescription = "Descrição do ícone",
                modifier = Modifier.size(10.dp),
                tint = Color.Unspecified
            )
        }


    }
}
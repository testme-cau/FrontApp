package com.example.testme.ui.screens.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.testme.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamRow(
    exam: SubjectExamUi,
    isDeleting: Boolean,
    onTakeExam: () -> Unit,
    onViewResult: () -> Unit,
    onDelete: () -> Unit,
    difficultyLabel: String,
    activeJobLabel: String?
) {
    val brandSecondaryText = Color(0xFF4C6070)
    
    // 진행 중 상태: 생성 중이거나 채점 중인 경우
    val isGradingInProgress =
        exam.gradingJobStatus == "processing" || exam.gradingJobStatus == "pending"

    // 버튼 활성화 여부: View Result 또는 Take Exam이 가능할 때 true
    val primaryEnabled = exam.canViewResult || exam.canTakeExam

    // 날짜 포매팅
    val formattedDate = remember(exam.createdAt) {
        try {
            if (exam.createdAt != null) {
                // ISO 8601 파싱 (예: 2025-12-06T04:33:47.981000+00:00)
                val zdt = ZonedDateTime.parse(exam.createdAt)
                // 현재 로케일에 맞는 형식 (MEDIUM: "2025. 12. 6." or "Dec 6, 2025")
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
                zdt.format(formatter)
            } else {
                ""
            }
        } catch (e: Exception) {
            exam.createdAt ?: ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        // Explicitly handle click and clip for correct ripple shape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = primaryEnabled && !isDeleting) { 
                    when {
                        exam.canViewResult -> onViewResult()
                        exam.canTakeExam -> onTakeExam()
                    }
                }
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 상단 뱃지 및 삭제 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 상태 뱃지 로직
                    if (exam.canViewResult && exam.score != null) {
                        StatusBadge(
                            text = stringResource(R.string.status_grading_completed),
                            backgroundColor = Color(0xFFE3F2FD), // Light Blue
                            contentColor = Color(0xFF1976D2) // Blue
                        )
                    } else if (exam.canTakeExam) {
                         StatusBadge(
                            text = stringResource(R.string.status_not_taken),
                            backgroundColor = Color(0xFFE8F5E9), // Light Green
                            contentColor = Color(0xFF388E3C) // Green
                        )
                    } else if (isGradingInProgress) {
                         StatusBadge(
                            text = stringResource(R.string.status_grading),
                            backgroundColor = Color(0xFFFFF3E0), // Orange
                            contentColor = Color(0xFFF57C00) // Orange
                        )
                    } else {
                         // Default / Processing or Completed without Score
                         val label = if (exam.canViewResult) {
                             stringResource(R.string.status_grading_completed)
                         } else {
                             activeJobLabel ?: stringResource(R.string.status_processing)
                         }
                         
                         val (bgColor, contentColor) = if (exam.canViewResult) {
                             Color(0xFFE3F2FD) to Color(0xFF1976D2) // Blue for Completed
                         } else {
                             Color(0xFFF5F5F5) to Color.Gray // Gray for Processing
                         }

                         StatusBadge(
                            text = label,
                            backgroundColor = bgColor,
                            contentColor = contentColor
                        )
                    }
                    
                    // 휴지통 아이콘 (우측 상단)
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.Gray
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onDelete() }
                        )
                    }
                }

                // 제목
                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 생성일 및 점수 표시
                if (exam.canViewResult && exam.score != null && exam.maxScore != null) {
                     Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                         Text(
                            text = stringResource(R.string.label_created) + " $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = brandSecondaryText
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = brandSecondaryText
                        )
                         Text(
                            text = "${exam.score.toInt()}/${exam.maxScore.toInt()}" + stringResource(R.string.label_points),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2962FF) // Strong Blue for score
                            )
                        )
                    }
                } else {
                    // 날짜만 표시
                     Text(
                        text = stringResource(R.string.label_created) + " $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = brandSecondaryText
                    )
                }
                
                // PDF 소스 (있을 경우)
                if (!exam.pdfName.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                         Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Default.Description,
                                 contentDescription = null,
                                 modifier = Modifier.size(14.dp),
                                 tint = Color.Gray
                             )
                             Text(
                                 text = exam.pdfName,
                                 style = MaterialTheme.typography.labelSmall,
                                 color = Color.Gray,
                                 maxLines = 1,
                                 overflow = TextOverflow.Ellipsis
                             )
                         }
                    }
                }

                // 메타데이터 아이콘 + 레이블
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 문항 수
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = brandSecondaryText
                        )
                        Text(
                            text = "${exam.numQuestions}", 
                            style = MaterialTheme.typography.bodySmall,
                            color = brandSecondaryText
                        )
                    }

                    // 총점 (100pts)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                         Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = brandSecondaryText
                        )
                         Text(
                            text = "100" + stringResource(R.string.label_points), // "100점"
                            style = MaterialTheme.typography.bodySmall,
                            color = brandSecondaryText
                        )
                    }
                    
                    // 예상 시간
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Schedule, 
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = brandSecondaryText
                        )
                        Text(
                            text = stringResource(R.string.label_est_time) + ": 60" + stringResource(R.string.label_minutes), 
                            style = MaterialTheme.typography.bodySmall,
                            color = brandSecondaryText
                        )
                    }

                    // 난이도 뱃지
                    StatusBadge(
                        text = difficultyLabel,
                        backgroundColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF388E3C)
                    )
                }
                
                // 진행 바 (생성/채점 중)
                if (exam.hasOngoingJob && exam.progress != null && activeJobLabel != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val pDouble = exam.progress!!.coerceIn(0.0, 100.0)
                        val pFloat = (pDouble / 100.0).toFloat()

                        LinearProgressIndicator(
                            progress = pFloat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = stringResource(R.string.job_progress_fmt, pDouble.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = brandSecondaryText
                        )
                    }
                }

                // 하단 버튼 (응시하기 / 결과 보기)
                if (exam.canTakeExam) {
                    Button(
                        onClick = { onTakeExam() },
                        enabled = primaryEnabled && !isDeleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BFA5), // Teal Green
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_take_exam),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (exam.canViewResult) {
                    Button(
                        onClick = { onViewResult() },
                        enabled = primaryEnabled && !isDeleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF212121), // Dark / Black
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_view_result),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
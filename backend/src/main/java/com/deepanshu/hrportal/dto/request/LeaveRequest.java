package com.deepanshu.hrportal.dto.request;

import com.deepanshu.hrportal.model.LeaveRequest.LeaveType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

public class LeaveRequest {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Create {
        @NotNull(message = "Leave type is required")
        private LeaveType leaveType;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @NotBlank(message = "Reason is required")
        @Size(min = 10, max = 500, message = "Reason must be 10-500 characters")
        private String reason;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApproveReject {
        @Size(max = 500, message = "Comment must be at most 500 characters")
        private String comment;
    }
}

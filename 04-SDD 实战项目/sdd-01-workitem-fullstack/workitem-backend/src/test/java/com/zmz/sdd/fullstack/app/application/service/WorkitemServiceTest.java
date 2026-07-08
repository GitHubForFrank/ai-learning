package com.zmz.sdd.fullstack.app.application.service;

import com.zmz.sdd.fullstack.app.application.service.impl.WorkitemServiceImpl;
import com.zmz.sdd.fullstack.app.domain.model.Workitem;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemPriority;
import com.zmz.sdd.fullstack.app.domain.model.WorkitemStatus;
import com.zmz.sdd.fullstack.app.domain.repository.WorkitemRepository;
import com.zmz.sdd.fullstack.core.common.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 04-验收标准.md §4.1 TC-U-01 ~ TC-U-08]
 */
@ExtendWith(MockitoExtension.class)
class WorkitemServiceTest {

    @Mock WorkitemRepository repo;
    @InjectMocks WorkitemServiceImpl service;

    @Test
    @DisplayName("TC-U-01 BR-01/02: create should set defaults (status=TODO, priority=MEDIUM)")
    void create_shouldSetDefaults() {
        when(repo.save(any())).thenAnswer(inv -> {
            Workitem t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        Workitem created = service.create("hello", null, null, null);
        assertThat(created.getStatus()).isEqualTo(WorkitemStatus.TODO);
        assertThat(created.getPriority()).isEqualTo(WorkitemPriority.MEDIUM);
    }

    @Test
    @DisplayName("TC-U-05 BR-06: update DONE workitem changing priority should throw 1003")
    void update_shouldThrow1003_whenDoneWorkitemChangesPriority() {
        Workitem done = Workitem.builder().id(1L).title("x").status(WorkitemStatus.DONE)
                .priority(WorkitemPriority.LOW).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(repo.findById(1L)).thenReturn(Optional.of(done));
        assertThatThrownBy(() -> service.update(1L, null, null, null, WorkitemPriority.HIGH, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Done workitem");
    }

    @Test
    @DisplayName("TC-U-06 BR-06 boundary: DONE allows status change only")
    void update_shouldAllow_whenDoneWorkitemOnlyChangesStatus() {
        Workitem done = Workitem.builder().id(1L).title("x").status(WorkitemStatus.DONE)
                .priority(WorkitemPriority.LOW).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(repo.findById(1L)).thenReturn(Optional.of(done));
        when(repo.update(any())).thenAnswer(inv -> inv.getArgument(0));
        Workitem updated = service.update(1L, null, null, WorkitemStatus.DOING, null, null);
        assertThat(updated.getStatus()).isEqualTo(WorkitemStatus.DOING);
    }

    @Test
    @DisplayName("TC-U-08 BR-07: delete should soft-delete (repo returns true)")
    void delete_shouldSoftDelete_notPhysical() {
        when(repo.softDeleteById(1L)).thenReturn(true);
        service.delete(1L);
        // 无异常 → 通过;实际 deleted=1 物理断言由 TC-R-DB-* 覆盖
    }
}

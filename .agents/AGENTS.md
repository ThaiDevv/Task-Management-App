# Git Commit Message Format Rules

When generating or executing git commits for this workspace, ALWAYS enforce the following commit message format:

```
[TYPE] Short description
```

### Allowed Commit Types:
- `[FEAT]`    : New feature / Tính năng mới
- `[FIX]`     : Bug fix / Sửa lỗi
- `[UI]`      : User interface change / Thay đổi giao diện
- `[REFACTOR]`: Code refactoring / Tái cấu trúc code
- `[TEST]`    : Add or modify tests / Thêm hoặc sửa test
- `[DOCS]`    : Documentation updates / Tài liệu
- `[CHORE]`   : Configuration, build, or dependency updates / Config, build, dependencies

### Examples:
- `[FEAT] Add create task with validation`
- `[FIX] Fix alarm not rescheduled after reboot`
- `[UI] Add empty state for task list`
- `[TEST] Add unit tests for TaskRepository`

### Constraints:
- Never use generic commit messages like "update", "fix", "final".
- Always clearly describe what changes were made in concise language.

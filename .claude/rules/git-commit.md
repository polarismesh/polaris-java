# Git 提交规范

本文件为 polaris-java 项目级 Git 提交规范。所有提交均须遵循。

## Commit Message 格式

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>: <subject>
```

常用 type：

| type     | 含义           |
|----------|--------------|
| feat     | 新增功能         |
| fix      | 修复 bug       |
| refactor | 重构（不改变外部行为）  |
| test     | 仅测试代码改动      |
| docs     | 仅文档改动        |
| chore    | 构建脚本、依赖、杂项改动 |

变量名、commit message 必须使用英文。

## 必须包含的元数据

每次 commit **必须**包含两行尾注：

1. **Signed-off-by**（DCO 签名）：使用 `git commit -s` 自动添加。这是 polarismesh 上游 DCO 检查的硬性要求，缺失 PR 无法合并。
2. **Co-Authored-By**：根据本次实际使用的模型、版本和上下文长度填写，格式为：

   ```
   Co-Authored-By: <模型名> <版本> (<上下文长度>) <noreply@anthropic.com>
   ```

   例如：

   ```
   Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
   ```

## 提交命令模板

为保证 message 多行格式正确，使用 HEREDOC 传入：

```bash
git commit -s -m "$(cat <<'EOF'
feat: short subject in english

Optional body explaining the why.

Co-Authored-By: <模型名> <版本> (<上下文长度>) <noreply@anthropic.com>
EOF
)"
```

只有单行 message 时也可简写：

```bash
git commit -s -m "fix: avoid NPE in ConsumerAPI.getInstances"
```

但 `Co-Authored-By` 仍需通过 `-m` 追加或采用 HEREDOC 形式补全。

## 禁令

- **禁止** 使用 `--no-verify` / `--no-gpg-sign` 绕过 hook 与签名，除非用户明确要求
- 不得在 commit 中包含 `.env`、凭据、密钥等敏感文件

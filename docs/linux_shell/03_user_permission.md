# 第3章 用户与权限管理

## 用户与组管理

```bash
useradd testuser
passwd testuser
groupadd dev
usermod -aG dev testuser
id testuser
```

## 权限表示

`drwxr-xr-x`

- d 表示目录
- rwx：读、写、执行权限

## 权限修改

```bash
chmod 755 file.sh
chown root:root file.sh
```

## 练习题

1. 创建一个用户 devuser 并设置密码。
2. 修改 test.txt 文件权限为 rw-r-----。

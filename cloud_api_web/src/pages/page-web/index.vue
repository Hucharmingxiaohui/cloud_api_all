<template>
  <div class="login">
    <div class="header">
      <div class="name">
        无人机任务规划与数据采集系统
      </div>
    </div>
    <div class="login-wrapper">
      <h4 style="text-align: left; margin: 0; color: #fff; font-weight: 600">
        登录：
      </h4>
      <el-form
        :model="formState"
        :rules="loginRules"
        ref="loginForm"
        class="login-form"
        @keyup.enter="onSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formState.username"
            type="text"
            auto-complete="off"
            placeholder="用户名"
            style="position: relative"
          >
          <template v-slot:prefix>
            <i class="login-tagger">
              <img src="../../assets/icons/login-user.png" />
            </i>
          </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="formState.password"
            type="password"
            auto-complete="off"
            placeholder="密码"
          >
            <template v-slot:prefix>
              <i class="login-tagger">
                <img src="../../assets/icons/locked.png" />
              </i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item style="width: 100%">
          <el-button
            :loading="loading"
            :disabled="loginBtnDisabled"
            size="medium"
            type="primary"
            style="
              width: 100%;
              background: #1ab394;
              border-radius: 3px;
              height: 38px;
              border-color: #1ab394;
            "
            @click="onSubmit">
            <span v-if="!loading" style="position: relative; bottom: 2px"> 登 录</span>
            <span v-else>登 录 中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { LockOutlined, UserOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { reactive, computed, UnwrapRef, ref } from 'vue'
import { login, LoginBody } from '/@/api/manage'
import { getRoot } from '/@/root'
import { ELocalStorageKey, ERouterName, EUserType } from '/@/types'
import router from '/@/router'
import { CURRENT_CONFIG } from '/@/api/http/config'
import { log } from 'console'

const root = getRoot()

const formState: UnwrapRef<LoginBody> = reactive({
  username: '',
  password: '',
  flag: EUserType.Web,
})

const loginRules = {
  username: [
    { required: true, trigger: 'blur', message: '请输入您的账号' },
  ],
  password: [
    { required: true, trigger: 'blur', message: '请输入您的密码' },
  ]
}
const loading = ref(false)
const loginBtnDisabled = computed(() => {
  return !formState.username || !formState.password
})

const loginForm = ref()
const onSubmit = async (e: any) => {
  // if (loginBtnDisabled.value) {
  //   return
  // }
  // 进行表单验证
  const valid = await loginForm.value.validate()

  if (!valid) {
    return
  }
  try {
    loading.value = true
    const result = await login(formState)
    if (result.code === 0) {
      localStorage.setItem(ELocalStorageKey.Token, result.data.access_token)
      localStorage.setItem(ELocalStorageKey.WorkspaceId, result.data.workspace_id)
      localStorage.setItem(ELocalStorageKey.Username, result.data.username)
      localStorage.setItem(ELocalStorageKey.UserId, result.data.user_id)
      localStorage.setItem(ELocalStorageKey.Flag, EUserType.Web.toString())
      localStorage.setItem(ELocalStorageKey.User_Type, result.data.user_type)
      // 跳转到成员页面
      root.$router.push(ERouterName.NEW_WAYLINE)
    } else {
      loading.value = false
      message.error(result.message)
    }
  } catch {
    loading.value = false
  }
}

</script>

<style lang="scss" scoped>
@import '/@/styles/index.scss';
// .login {
//   background-image: url('/@/assets/icons/login.png');
//   background-color: rgb(36, 118, 231);
//   background-attachment: fixed;
//   background-repeat: no-repeat;
//   background-size: cover;
//   -webkit-background-size: cover;
//   -moz-background-size: cover;
//   // background-color: $dark-highlight;
//   height: 100vh;
// }

.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url('/@/assets/icons/login.png');
  background-repeat: no-repeat;
  background-size: 100% 100%;
  background-position: center;
  text-align: center;
  .header {
    position: relative;
    bottom: 200px;
    .name {
      font-size: 45px;
      color: #fff;
      margin-left: 20px;
      position: relative;
      bottom: 80px;
    }
  }
}
:deep(.el-input__wrapper){
  background: rgba(59, 116, 255, 0.15);
  box-shadow: inset 0 0 0.9375rem 0.0625rem rgba(34, 135, 255, 0.5);
  border: 0.0625rem solid #719fff;
}
:deep(.el-input__inner){
  color: #ffffff;
  //改变浏览器自动填充的颜色
  -webkit-text-fill-color: #ededed !important;
  background-color: transparent !important;
  transition: background-color 50000s ease-in-out 0s;
}
:deep(.el-input) {
    height: 38px;

  }
   //改变input自动填充背景颜色
input:-internal-autofill-previewed,
input:-internal-autofill-selected {
  -webkit-text-fill-color: #808080;
  transition: background-color 1000s ease-out 0.5s;
}

.login-wrapper {
    position: absolute;
    background: rgba(255, 255, 255, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.3);
    box-shadow: 0 3px 0 rgb(12 12 12 / 3%);
    border-radius: 3px;
    padding: 30px;
    height: 317px;
    width: 380px;
  }

  .login-form {
  margin-top: 20px;
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
  .login-tagger {
    position: absolute;
    left: 285px;
    top: 3px;
  }
}
</style>

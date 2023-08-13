<template>
  <div class="play-list-container">
    <yin-nav :styleList="recommendMethod" :activeName="activeName" @click="handleChangeView"></yin-nav>
    <song-list-f-r :playList="data" path="song-sheet-detail"></song-list-f-r>
    <el-pagination
      class="pagination"
      background
      layout="total, prev, pager, next"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="allSong.length"
      @current-change="handleCurrentChange"
    >
    </el-pagination>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from "vue";
import YinNav from "@/components/layouts/YinNav.vue";
// import PlayList from "@/components/PlayList.vue";
import SongListFR from "@/components/SongListFR.vue";
import { HttpManager } from "@/api";
import {RECOMMENDATIONMETHOD} from "@/enums";
import {useStore} from "vuex";
import { ElMessage } from 'element-plus'
export default defineComponent({
  components: {
    YinNav,
      SongListFR,
  },
  setup() {
    const store = useStore();
    const activeName = ref("全部歌曲");
    const pageSize = ref(15); // 页数
    const currentPage = ref(1); // 当前页
    const recommendMethod = ref(RECOMMENDATIONMETHOD); // 歌单导航栏类别
    const allSong = ref([]); // 歌单
    const data = computed(() => allSong.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value));
    const uid = computed(() => store.getters.userId).value;
    // 获取全部歌单
    async function getSongList() {
      allSong.value = ((await HttpManager.getAllSong()) as ResponseBody).data;
      currentPage.value = 1;
    }
    // 通过推荐方法获取歌单
    async function getSongListOfStyle(method) {
      allSong.value = ((await HttpManager.getSongByMethod(method)) as ResponseBody).data;
      currentPage.value = 1;
    }
      async function getSongByUid(uid, method) {
        if (uid === ""){
            ElMessage.error({
                showClose: true,
                message: '请登陆以获取'+ method
            })
        }else if ((await HttpManager.getSongByUid(uid,method) as ResponseBody).type ==="error"){
            ElMessage.info({
                showClose: true,
                message: (await HttpManager.getSongByUid(uid,method) as ResponseBody).message
            })
        }else{
              allSong.value = ((await HttpManager.getSongByUid(uid,method)) as ResponseBody).data;
              currentPage.value = 1;
          }

      }

    try {
      getSongList();
    } catch (error) {
      console.error(error);
    }

    // 获取歌单
    async function handleChangeView(item) {
      activeName.value = item.name;
      allSong.value = [];
      try {
        if (item.name === "全部歌曲") {
          await getSongList();
        }else if(item.name === "私人推荐" || item.name === "流式推荐"){
          await getSongByUid(uid, item.name);
        }
        else {
          await getSongListOfStyle(item.name);
        }
      } catch (error) {
        console.error(error);
      }
    }
    // 获取当前页
    function handleCurrentChange(val) {
      currentPage.value = val;
    }
    return {
      activeName,
      recommendMethod,
      pageSize,
      currentPage,
      allSong,
      data,
      handleChangeView,
      handleCurrentChange,
    };
  },
});
</script>

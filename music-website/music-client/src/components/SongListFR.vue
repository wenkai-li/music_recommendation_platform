<template>
  <div class="song-list-fr">
    <div class="song-title" v-if="title">{{ title }}</div>
    <ul class="play-body">
      <li class="card-frame" v-for="(item, index) in playList" :key="index">
        <div class="card" @click="playSong(item)">
          <el-image class="card-img" fit="contain" :src="attachImageUrl(item.pic)" />
          <div class="mask" @click="playSong(item)">
            <yin-icon class="mask-icon" :icon="BOFANG"></yin-icon>
          </div>
        </div>
        <p class="card-name">{{ item.name || item.title }}</p>
          <p v-if="item.count" class="card-name">评分数量: {{ parseInt(item.count) }}</p>
          <p v-if="item.score" class="card-name">推荐指数: {{ parseFloat(item.score).toFixed(3) }}</p>
      </li>
    </ul>
  </div>
</template>

<script lang="ts">
import {defineComponent, getCurrentInstance, ref, toRefs} from "vue";
import YinIcon from "@/components/layouts/YinIcon.vue";
import mixin from "@/mixins/mixin";
import {Icon, RouterName} from "@/enums";
import { HttpManager } from "@/api";

export default defineComponent({
  components: {
      // eslint-disable-next-line vue/no-unused-components
    YinIcon
  },
  props: {
    title: String,
    playList: Array,
    path: String,
  },
  setup(props) {
    const { proxy } = getCurrentInstance();
    const { routerManager , justPlayMusic} = mixin();
    const songDetail = ref([]);
    const { path } = toRefs(props);

    // function playSong(item) {
    //   // proxy.$store.commit("setSongDetails", item);
    //   // routerManager(path.value, { path: `/${path.value}/${item.id}` });
    //     console.log(item.mid)
    //   routerManager(RouterName.Lyric, { path: `${RouterName.Lyric}/${item.mid}` });
    // }

      async function playSong(item) {
          songDetail.value = ((await HttpManager.getSongOfId(item.mid)) as ResponseBody).data
          justPlayMusic({
              id: songDetail.value[0].id,
              url: songDetail.value[0].url,
              pic: songDetail.value[0].pic,
              name: songDetail.value[0].name,
              lyric: songDetail.value[0].lyric
          });

      }

      return {
      BOFANG: Icon.BOFANG,
      playSong,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.song-list-fr {
  padding: 0 1rem;

  .song-title {
    height: 60px;
    line-height: 60px;
    font-size: 28px;
    font-weight: 500;
    text-align: center;
    color: $color-black;
    box-sizing: border-box;
  }

  .play-body {
    @include layout(flex-start, stretch, row, wrap);
  }
}

.card-frame {
  .card {
    position: relative;
    height: 0;
    padding-bottom: 100%;
    overflow: hidden;
    border-radius: 5px;

    .card-img {
      width: 100%;
      transition: all 0.4s ease;
    }
  }

  .card-name {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    margin: 0.5rem 0;
    color: #4f077c;
  }

  &:hover .card-img {
    transform: scale(1.2);
  }
}

.mask {
  position: absolute;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 5px;
  background-color: rgba(52, 47, 41, 0.4);
  @include layout(center, center);
  transition: all 0.3s ease-in-out;
  opacity: 0;

  .mask-icon {
    @include icon(2em, rgba(240, 240, 240, 1));
  }

  &:hover {
    opacity: 1;
    cursor: pointer;
  }
}

@media screen and (min-width: $sm) {
  .card-frame {
    width: 18%;
    margin: 0.5rem 1%;
  }
}

@media screen and (max-width: $sm) {
  .card-frame {
    width: 46%;
    margin: 0.5rem 2%;
  }
}
</style>

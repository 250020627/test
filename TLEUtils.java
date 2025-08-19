package com.orbitmonitor.tle;

import org.hipparchus.util.FastMath;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.File;

public class TLEUtils {

    // 封装的方法，返回瞬时吻切轨道根数
    public static String getTleParams(String line1, String line2, AbsoluteDate startDate, AbsoluteDate endDate) {
        StringBuilder result = new StringBuilder();
        try {
            TLE tle = new TLE(line1, line2);

            result.append("TLE轨道时间: ").append(tle.getDate()).append("\n");
            result.append("TLE line 1: ").append(line1).append("\n");
            result.append("TLE line 2: ").append(line2).append("\n\n");
            result.append(String.format("%-27s %-18s %-18s %-18s %-18s %-18s %-18s\n",
                    "时间 (UTC)", "半长轴 (km)", "偏心率", "轨道倾角 (deg)", "升交点赤经 (deg)", "近地点幅角 (deg)", "平近点角 (deg)"));

            TLEPropagator sgp4 = SGP4.selectExtrapolator(tle);
            double stepSize = 3600.0;
            AbsoluteDate currentDate = startDate;
            while (currentDate.compareTo(endDate) <= 0) {
                // TOD (True of Date) 参考系与 STK 中常用的 "TrueOfDate" 坐标系一致
                var todFrame = FramesFactory.getTOD(IERSConventions.IERS_2010, true);
                TimeStampedPVCoordinates pv = sgp4.getPVCoordinates(currentDate, todFrame);
                KeplerianOrbit orbit = new KeplerianOrbit(pv, todFrame, Constants.WGS84_EARTH_MU);
                double A = orbit.getA() / 1000.0; // 半长轴, km
                double E = orbit.getE(); // 偏心率
                double I = FastMath.toDegrees(orbit.getI()); // 轨道倾角
                double RAAN = Math.toDegrees(orbit.getRightAscensionOfAscendingNode()); // 升交点赤经
                if (RAAN < 0) {
                    RAAN += 360.0;
                }
                double W = Math.toDegrees(orbit.getPerigeeArgument()); // 近地点幅角
                if (W < 0) {
                    W += 360.0;
                }
                double M = Math.toDegrees(orbit.getMeanAnomaly()); // 平近点角
                if (M < 0) {
                    M += 360.0;
                }

                result.append(String.format("%-27s %-18.5f %-18.9f %-18.9f %-18.9f %-18.9f %-18.9f\n",
                        currentDate.toString(), A, E, I, RAAN, W, M));

                currentDate = currentDate.shiftedBy(stepSize);
            }

        } catch (OrekitException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * 计算并返回Brouwer-Lyddane平根数的演化
     * 修正版本：正确处理SGP4内部的平根数
     */
    public static String getMeanTleParams(String line1, String line2, AbsoluteDate startDate, AbsoluteDate endDate) {
        StringBuilder result = new StringBuilder();
        try {
            TLE tle = new TLE(line1, line2);
            SGP4MeanElementsPropagator meanPropagator = new SGP4MeanElementsPropagator(tle);

            result.append(String.format("%-27s %-18s %-18s %-18s %-18s %-18s %-18s\n",
                    "时间 (UTC)", "半长轴 (km)", "偏心率", "轨道倾角 (deg)", "升交点赤经 (deg)", "近地点幅角 (deg)", "平近点角 (deg)"));

            double stepSize = 3600.0;
            AbsoluteDate currentDate = startDate;
            while (currentDate.compareTo(endDate) <= 0) {
                KeplerianOrbit meanOrbit = meanPropagator.getMeanElements(currentDate);

                double A = meanOrbit.getA() / 1000.0; // 半长轴, km
                double E = meanOrbit.getE(); // 偏心率
                double I = FastMath.toDegrees(meanOrbit.getI()); // 轨道倾角
                double RAAN = Math.toDegrees(meanOrbit.getRightAscensionOfAscendingNode()); // 升交点赤经
                if (RAAN < 0) {
                    RAAN += 360.0;
                }
                double W = Math.toDegrees(meanOrbit.getPerigeeArgument()); // 近地点幅角
                if (W < 0) {
                    W += 360.0;
                }
                double M = Math.toDegrees(meanOrbit.getMeanAnomaly()); // 平近点角
                if (M < 0) {
                    M += 360.0;
                }

                result.append(String.format("%-27s %-18.5f %-18.9f %-18.9f %-18.9f %-18.9f %-18.9f\n",
                        currentDate.toString(), A, E, I, RAAN, W, M));

                currentDate = currentDate.shiftedBy(stepSize);
            }

        } catch (OrekitException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static void main(String[] args) {
        try {
            // 1. 初始化Orekit数据环境
            File orekitDataDir = new File("./dat/orekit-data");
            if (!orekitDataDir.exists()) {
                System.err.println("警告: Orekit数据目录不存在，请修改为正确的路径: " + orekitDataDir.getAbsolutePath());
                // 尝试使用当前目录下的orekit-data文件夹
                orekitDataDir = new File("./orekit-data");
                if (!orekitDataDir.exists()) {
                    System.err.println("错误: 无法找到Orekit数据目录");
                    return;
                }
            }

            DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitDataDir));

            // 2. TLE数据
            String line1 = "1 58918U 24024B   25173.92752492  .00004806  00000-0  24653-3 0  9992";
            String line2 = "2 58918  97.3509 245.6212 0011797 302.3575  57.6516 15.16942229 76787";

            TLE tle = new TLE(line1, line2);

            // 打印从TLE中直接解析的Brouwer-Lyddane平根数
            System.out.println("--- 从TLE直接解析的Brouwer-Lyddane平根轨道根数 ---");
            System.out.println("TLE Epoch: " + tle.getDate());
            // 从平根运动计算半长轴
            double n = tle.getMeanMotion() * 2 * FastMath.PI / Constants.JULIAN_DAY; // rad/s
            double a = FastMath.cbrt(Constants.WGS84_EARTH_MU / (n * n));
            System.out.println("半长轴 (a): " + String.format("%.5f", a / 1000.0) + " km");
            System.out.println("偏心率 (e): " + String.format("%.7f", tle.getE()));
            System.out.println("轨道倾角 (i): " + String.format("%.4f", FastMath.toDegrees(tle.getI())) + " deg");
            System.out.println("升交点赤经 (RAAN): " + String.format("%.4f", FastMath.toDegrees(tle.getRaan())) + " deg");
            System.out.println("近地点幅角 (ω): " + String.format("%.4f", FastMath.toDegrees(tle.getPerigeeArgument())) + " deg");
            System.out.println("平近点角 (M): " + String.format("%.4f", FastMath.toDegrees(tle.getMeanAnomaly())) + " deg");
            System.out.println("----------------------------------------------------");
            System.out.println();

            // 3. 定义开始和结束日期 (从TLE历元开始，外推2小时)
            AbsoluteDate startDate = tle.getDate();
            AbsoluteDate endDate = startDate.shiftedBy(2 * 3600.0);

            // 4. 调用getTleParams方法进行外推和计算
            String results = TLEUtils.getTleParams(line1, line2, startDate, endDate);

            // 5. 输出结果
            System.out.println("--- 轨道外推结果 (瞬时吻切根数) ---");
            System.out.println(results);

            System.out.println("\n--- 轨道外推结果 (Brouwer-Lyddane平根数) ---");
            String meanResults = TLEUtils.getMeanTleParams(line1, line2, startDate, endDate);
            System.out.println(meanResults);

        } catch (Exception e) {
            System.err.println("处理TLE数据时发生错误:");
            e.printStackTrace();
        }
    }

    /**
     * 修正版的SGP4平根数提取器
     * 正确处理Brouwer-Lyddane平根数的计算
     */
    public static class SGP4MeanElementsPropagator extends SGP4 {

        private final TLE initialTLE;

        /**
         * 构造函数，使用默认参数调用父类构造函数。
         * @param initialTLE 初始TLE
         */
        public SGP4MeanElementsPropagator(final TLE initialTLE) {
            // 调用父类构造函数
            super(initialTLE, new FrameAlignedProvider(DataContext.getDefault().getFrames().getTEME()),
                    DEFAULT_MASS, DataContext.getDefault().getFrames().getTEME());
            this.initialTLE = initialTLE;
        }

        /**
         * 计算并返回指定日期的Brouwer-Lyddane平根数。
         * 修正版本：正确处理角度和平根数的提取
         * @param date 计算日期
         * @return 该日期的平根轨道
         */
        public KeplerianOrbit getMeanElements(final AbsoluteDate date) {
            // 计算自TLE历元以来的时间（以分钟为单位）
            final double tSince = date.durationFrom(this.getTLE().getDate()) / 60.0;

            // 调用父类的受保护方法来更新平根数
            sxpPropagate(tSince);

            // SGP4内部使用的是无量纲的半长轴（以地球半径为单位）
            final double ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
            final double semiMajorAxis = this.a * ae;  // 转换为米

            // 偏心率直接使用
            final double eccentricity = this.e;

            // 倾角直接使用（已经是弧度）
            final double inclination = this.i;

            // 升交点赤经（弧度）
            double raan = this.xnode;

            // 近地点幅角（弧度）
            double argPerigee = this.omega;

            // 计算平近点角
            // xl 是平经度 (mean longitude) = Ω + ω + M
            // 因此 M = xl - Ω - ω
            double meanAnomaly = this.xl - this.omega - this.xnode;

            // 将角度归一化到 [0, 2π) 范围
            raan = normalizeAngle(raan);
            argPerigee = normalizeAngle(argPerigee);
            meanAnomaly = normalizeAngle(meanAnomaly);

            // 创建Keplerian轨道对象
            return new KeplerianOrbit(
                    semiMajorAxis,
                    eccentricity,
                    inclination,
                    argPerigee,
                    raan,
                    meanAnomaly,
                    PositionAngleType.MEAN,
                    this.getFrame(),
                    date,
                    this.getMU()
            );
        }

        /**
         * 将角度归一化到 [0, 2π) 范围
         * @param angle 输入角度（弧度）
         * @return 归一化后的角度（弧度）
         */
        private double normalizeAngle(double angle) {
            double normalized = angle % (2.0 * FastMath.PI);
            if (normalized < 0) {
                normalized += 2.0 * FastMath.PI;
            }
            return normalized;
        }

        /**
         * 获取TLE中存储的原始平根数（用于对比验证）
         * @return TLE中的原始Keplerian轨道
         */
        public KeplerianOrbit getTLEMeanElements() {
            // 从平均运动计算半长轴
            double n = initialTLE.getMeanMotion() * 2 * FastMath.PI / Constants.JULIAN_DAY; // rad/s
            double a = FastMath.cbrt(Constants.WGS84_EARTH_MU / (n * n));

            // TLE中的偏心率是直接存储的
            double e = initialTLE.getE();

            // TLE中的角度（已经是弧度）
            double i = initialTLE.getI();
            double raan = initialTLE.getRaan();
            double argPerigee = initialTLE.getPerigeeArgument();
            double meanAnomaly = initialTLE.getMeanAnomaly();

            return new KeplerianOrbit(
                    a, e, i, argPerigee, raan, meanAnomaly,
                    PositionAngleType.MEAN,
                    this.getFrame(),
                    initialTLE.getDate(),
                    Constants.WGS84_EARTH_MU
            );
        }
    }
}
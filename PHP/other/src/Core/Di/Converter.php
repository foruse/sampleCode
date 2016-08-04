<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di;

use Core\Di\Converter\Argument\Type\ObjectType;
use Core\Di\Converter\Argument\TypeFactoryInterface;

/**
 * Class Converter
 */
class Converter
{
    const INTERFACES = 'interfaces';

    const INSTANCES = 'instances';

    /**
     * @var TypeFactoryInterface
     */
    private $typeFactory;

    /**
     * @var array
     */
    private $configuration = [];

    /**
     * Converter constructor.
     *
     * @param TypeFactoryInterface $typeFactory
     */
    public function __construct(TypeFactoryInterface $typeFactory)
    {
        $this->typeFactory = $typeFactory;
    }

    /**
     * @param array $configuration
     * @return array
     */
    public function convert(array $configuration)
    {
        $this->createInstance($configuration);
        $this->createPreference($configuration);

        return $this->configuration;
    }

    /**
     * @param array $data
     */
    private function createInstance(array & $data)
    {
        foreach ($data as $name => $list) {
            if (self::INSTANCES === $name) {
                foreach ($list as $item) {
                    $container = $this->typeFactory->create(ObjectType::TYPE_OBJECT)
                        ->convert($item);
                    $this->configuration[$container->getId()] = $container;
                }
            }
        }
    }

    /**
     * @param array $data
     */
    private function createPreference(array & $data)
    {
        foreach ($data as $name => $list) {
            if (self::INTERFACES === $name) {
                foreach ($list as $interface => $class) {
                    $container = $this->typeFactory->create(ObjectType::TYPE_OBJECT)
                        ->convert([ObjectType::CLASS_NAME => $class]);
                    $this->configuration[$interface] = $container;
                }
            }
        }
    }
}
